/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorTest {

    private static final InetSocketAddress REMOTE_ID_1 = new InetSocketAddress("10.1.0.2", 8080);

    @Test
    void shouldInitialiseState() {
        // Given
        ConsoleLogger logger = ConsoleLoggerTest.loggerForTest();
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(logger);

        // When
        Monitor monitor = new Monitor(
                logger,
                mock(MeshMonitor.class),
                timings,
                Duration.ofMillis(5),
                mock(SocketChannel.class),
                REMOTE_ID_1
        );

        // Then
        assertThat(monitor.isRunning()).isFalse();
        assertThat(monitor.getRemoteId()).isEqualTo(REMOTE_ID_1);
        assertThat(monitor.getTimings()).isEqualTo(timings);
    }

    @Test
    void shouldStartThreads() {
        // Given
        ConsoleLogger logger = ConsoleLoggerTest.loggerForTest();
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(logger);

        Socket socket = mock(Socket.class);
        when(socket.getRemoteSocketAddress()).thenReturn(REMOTE_ID_1);
        SocketChannel socketChannel = mock(SocketChannel.class);
        when(socketChannel.socket()).thenReturn(socket);

        // When
        Monitor monitor = new Monitor(
                logger,
                mock(MeshMonitor.class),
                timings,
                Duration.ofMillis(5),
                socketChannel,
                REMOTE_ID_1
        );
        monitor.start();

        // Then
        assertThat(monitor.isRunning()).isTrue();
        assertThat(Thread.getAllStackTraces().keySet())
                .extracting("name")
                .contains(
                        "/10.1.0.2:8080 receive thread",
                        "/10.1.0.2:8080 send thread"
                );
    }

    @Test
    void shouldStopBothThreadsOnIOExceptionAndNotifyMeshMonitor() throws IOException {
        // Given
        ConsoleLogger logger = ConsoleLoggerTest.loggerForTest();
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(logger);

        InetSocketAddress socketAddress = new InetSocketAddress("10.1.0.2", ThreadLocalRandom.current().nextInt() & 0xFFFF);
        Socket socket = mock(Socket.class);
        when(socket.getRemoteSocketAddress()).thenReturn(socketAddress);

        SocketChannel socketChannel = mock(SocketChannel.class);
        when(socketChannel.socket()).thenReturn(socket);
        when(socketChannel.read((ByteBuffer) any())).thenThrow(IOException.class);

        when(socketChannel.write((ByteBuffer) any())).thenAnswer(answer -> {
            ByteBuffer buffer = answer.getArgument(0);
            int remaining = buffer.remaining();

            buffer.position(buffer.position() + remaining);
            return remaining;
        });

        MeshMonitor meshMonitor = mock(MeshMonitor.class);

        // When
        Monitor monitor = new Monitor(
                logger,
                meshMonitor,
                timings,
                Duration.ofMillis(5),
                socketChannel,
                socketAddress
        );
        monitor.start();

        // Then
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertThat(monitor.isRunning()).isFalse();

            String address = socketAddress.toString();
            assertThat(Thread.getAllStackTraces().keySet())
                    .extracting("name")
                    .doesNotContain(
                            address + " receive thread",
                            address + " send thread"
                    );

            verify(meshMonitor).onDisconnect(eq(socketAddress), any());
        });
    }
}
