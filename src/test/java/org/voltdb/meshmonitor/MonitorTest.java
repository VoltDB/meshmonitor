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
import org.testcontainers.shaded.com.google.common.util.concurrent.Futures;
import org.testcontainers.shaded.com.google.common.util.concurrent.Uninterruptibles;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MonitorTest {

    private static final InetSocketAddress REMOTE_ID_1 = address("10.1.0.2", 8080);

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

        InetSocketAddress socketAddress = address("10.1.0.2");
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

    @Test
    void shouldSendAndReceivePing() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        // Given
        ConsoleLogger logger = ConsoleLoggerTest.loggerForTest();
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(logger);

        InetSocketAddress nodeA = address("127.0.0.1");
        InetSocketAddress nodeB = address("127.0.0.1");

        ServerSocketChannel nodeBChannel = ServerSocketChannel.open();
        nodeBChannel.socket().bind(nodeB);
        Future<SocketChannel> nodeAConnection = Executors.newFixedThreadPool(1).submit(nodeBChannel::accept);

        SocketChannel connectionToNodeB = SocketChannel.open(nodeB);

        MeshMonitor meshMonitor1 = mock(MeshMonitor.class);
        MeshMonitor meshMonitor2 = mock(MeshMonitor.class);

        List<InetSocketAddress> ipList = List.of(
                address("10.1.0.2", 42),
                address("10.1.0.3", 42),
                address("10.1.0.4", 42)
        );

        when(meshMonitor1.getConnections()).thenReturn(ipList);
        when(meshMonitor2.getConnections()).thenReturn(ipList);

        // When
        Monitor monitor1 = new Monitor(
                logger,
                meshMonitor1,
                timings,
                Duration.ofMillis(5),
                connectionToNodeB,
                nodeB
        );
        monitor1.start();

        Monitor monitor2 = new Monitor(
                logger,
                meshMonitor2,
                timings,
                Duration.ofMillis(5),
                nodeAConnection.get(5, TimeUnit.SECONDS),
                nodeA
        );
        monitor2.start();

        // Then
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            verify(meshMonitor1, atLeast(1)).onNewNodeInMesh(nodeB, ipList);
            verify(meshMonitor2, atLeast(1)).onNewNodeInMesh(nodeA, ipList);
        });
    }

    @Test
    void shouldReportDisconnect() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        // Given
        ConsoleLogger logger = ConsoleLoggerTest.loggerForTest();
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(logger);

        InetSocketAddress nodeB = address("127.0.0.1");

        ServerSocketChannel nodeBChannel = ServerSocketChannel.open();
        nodeBChannel.socket().bind(nodeB);
        Future<SocketChannel> nodeAConnection = Executors.newFixedThreadPool(1).submit(nodeBChannel::accept);

        SocketChannel connectionToNodeB = SocketChannel.open(nodeB);
        Futures.getUnchecked(nodeAConnection);

        MeshMonitor meshMonitor1 = mock(MeshMonitor.class);
        MeshMonitor meshMonitor2 = mock(MeshMonitor.class);

        List<InetSocketAddress> ipList = List.of(
                address("10.1.0.2", 42),
                address("10.1.0.3", 42),
                address("10.1.0.4", 42)
        );

        when(meshMonitor1.getConnections()).thenReturn(ipList);
        when(meshMonitor2.getConnections()).thenReturn(ipList);

        // When
        Monitor monitor1 = new Monitor(
                logger,
                meshMonitor1,
                timings,
                Duration.ofMillis(5),
                connectionToNodeB,
                nodeB
        );
        monitor1.start();

        nodeAConnection.get(5, TimeUnit.SECONDS);
        IOUtils.closeQuietly(nodeBChannel);
        IOUtils.closeQuietly(connectionToNodeB);

        // Then
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertThat(monitor1.isRunning()).isFalse();

            // Invoked by both sending and receiving thread
            verify(meshMonitor1, times(2)).onDisconnect(eq(nodeB), any());
        });
    }

    public static InetSocketAddress address(String hostname, int port) {
        return new InetSocketAddress(hostname, port & 0xFFFF);
    }

    public static InetSocketAddress address(String hostname) {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            return address(hostname, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
