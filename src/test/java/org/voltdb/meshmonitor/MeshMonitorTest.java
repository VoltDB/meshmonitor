/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Durations;
import org.voltdb.meshmonitor.serdes.IpPortSerializer;
import org.voltdb.meshmonitor.serdes.PacketSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;
import static org.voltdb.meshmonitor.MonitorTest.address;

class MeshMonitorTest {

    @Test
    void shouldReturnErrorWhenUnableToBindMainSocket() {
        // Given
        InetSocketAddress localAddress = address("255.0.0.1");
        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(),
                mock(ServerManager.class),
                localAddress,
                List.of(),
                5,
                50
        );

        // When
        int actual = meshMonitor.start(true);

        // Then
        assertThat(actual).isNotZero();
    }

    @Test
    void shouldPersistentlyTryToConnect() throws IOException, ExecutionException, InterruptedException {
        // Given
        InetSocketAddress localAddress = address("127.0.0.1");
        List<InetSocketAddress> addresses = List.of(
                address("127.0.0.1"),
                address("127.0.0.1"),
                address("127.0.0.1")
        );

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(),
                mock(ServerManager.class),
                localAddress,
                addresses,
                5,
                50
        );

        // When
        Executors.newFixedThreadPool(1).submit(() -> meshMonitor.start(false));

        List<Future<SocketChannel>> sockets = new ArrayList<>();
        for (InetSocketAddress address : addresses) {
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(address);

            Future<SocketChannel> connection = Executors.newFixedThreadPool(1).submit(serverSocket::accept);
            sockets.add(connection);
        }

        // Then
        for (Future<SocketChannel> maybeSocket : sockets) {
            SocketChannel socket = maybeSocket.get();
            ByteBuffer buffer = PacketSerializer.readPacketFully(socket);
            InetSocketAddress remoteAddress = IpPortSerializer.deserializeSingleIp(buffer);

            assertThat(remoteAddress).isEqualTo(localAddress);
        }
    }

    @Test
    void shouldAcceptConnectionsAndAddThemToTheMesh() {
        // Given
        InetSocketAddress localAddress = address("127.0.0.1");
        List<InetSocketAddress> addresses = List.of(
                address("127.0.0.1"),
                address("127.0.0.1"),
                address("127.0.0.1")
        );

        MonitorFactory monitorFactory = (_, _, _, _, _, remoteId) -> new FakeMonitor(remoteId);
        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                Duration.ofMillis(5)
        );

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(),
                serverManager,
                localAddress,
                List.of(),
                5,
                50
        );

        // When
        Executors.newFixedThreadPool(1).submit(() -> meshMonitor.start(false));

        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            for (InetSocketAddress address : addresses) {
                SocketChannel channel = SocketChannel.open(localAddress);
                PacketSerializer.writeHelloMessage(channel, address);
            }
        });

        // Then
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() ->
                assertThat(serverManager.getConnections()).containsExactlyElementsOf(addresses));
    }

    @Test
    void shouldRejectDuplicateConnection() throws IOException {
        // Given
        InetSocketAddress localAddress = address("127.0.0.1");
        InetSocketAddress remoteAddress = address("127.0.0.1");

        MonitorFactory monitorFactory = (_, _, _, _, _, remoteId) -> new FakeMonitor(remoteId);
        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                Duration.ofMillis(5)
        );

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(),
                serverManager,
                localAddress,
                List.of(),
                5,
                50
        );

        // When
        Executors.newFixedThreadPool(1).submit(() -> meshMonitor.start(false));

        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            SocketChannel channel = SocketChannel.open(localAddress);
            PacketSerializer.writeHelloMessage(channel, remoteAddress);
        });

        SocketChannel channel = SocketChannel.open(localAddress);
        PacketSerializer.writeHelloMessage(channel, remoteAddress);

        // Then
        await().atMost(Durations.TEN_MINUTES).untilAsserted(() ->
                assertThat(serverManager.getConnections()).containsOnly(remoteAddress));
    }

    @Test
    void shouldNotInitiateConnectionToItself() {
        // Given
        InetSocketAddress localAddress = address("127.0.0.1");
        InetSocketAddress remoteAddress = address("127.0.0.1");

        MonitorFactory monitorFactory = (_, _, _, _, _, remoteId) -> new FakeMonitor(remoteId);
        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                Duration.ofMillis(5)
        );

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(),
                serverManager,
                localAddress,
                List.of(),
                5,
                50
        );

        // When
        meshMonitor.onNewNodeInMesh(remoteAddress, List.of(localAddress));

        // Then
        assertThat(serverManager.getConnections()).isEmpty();
    }

    @Test
    void shouldRemoveConnectionOnDisconnect() throws IOException {
        // Given
        InetSocketAddress localAddress = address("127.0.0.1");
        InetSocketAddress remoteAddress = address("127.0.0.1");

        MonitorFactory monitorFactory = (_, _, _, _, _, remoteId) -> new FakeMonitor(remoteId);
        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                Duration.ofMillis(5)
        );

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(),
                serverManager,
                localAddress,
                List.of(),
                5,
                50
        );

        // When
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(remoteAddress);
        Executors.newFixedThreadPool(1).submit(serverSocket::accept);

        meshMonitor.onNewNodeInMesh(remoteAddress, List.of(remoteAddress));
        assertThat(serverManager.getConnections()).containsOnly(remoteAddress);

        // Then
        meshMonitor.onDisconnect(remoteAddress, new IOException());
        assertThat(serverManager.getConnections()).isEmpty();
    }

    @Test
    void shouldNotPrintStatisticsHeaderWhenNoMonitors() {
        // Given
        StringWriter logContent = new StringWriter();
        InetSocketAddress localAddress = address("127.0.0.1");

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(logContent),
                mock(ServerManager.class),
                localAddress,
                List.of(),
                1,
                50
        );

        // When
        Executors.newFixedThreadPool(1).submit(() -> meshMonitor.start(true));

        // Given
        await().atMost(TEN_SECONDS).untilAsserted(() -> {
            assertThat(logContent.toString()).contains("Connected to 0 servers");
            assertThat(logContent.toString()).doesNotContain("----------ping-(ms)----------");
        });
    }

    @Test
    void shouldPrintStatisticsWhenMonitorsPresent() {
        // Given
        StringWriter logContent = new StringWriter();
        InetSocketAddress localAddress = address("127.0.0.1");

        List<Monitor> fakeMonitors = List.of(
                FakeMonitor.random(),
                FakeMonitor.random()
        );
        ServerManager serverManager = mock(ServerManager.class);
        when(serverManager.getMonitors()).thenReturn(fakeMonitors);

        MeshMonitor meshMonitor = new MeshMonitor(
                loggerForTest(logContent),
                serverManager,
                localAddress,
                List.of(),
                1,
                50
        );

        // When
        Executors.newFixedThreadPool(1).submit(() -> meshMonitor.start(true));

        // Given
        await().atMost(TEN_SECONDS).untilAsserted(() -> {
            assertThat(logContent.toString()).contains("Connected to 2 servers");
            assertThat(logContent.toString()).contains("----------ping-(ms)----------");

            for (Monitor fakeMonitor : fakeMonitors) {
                InetSocketAddress remoteId = fakeMonitor.getRemoteId();
                assertThat(logContent.toString()).contains(remoteId.getHostString());
            }
        });
    }
}
