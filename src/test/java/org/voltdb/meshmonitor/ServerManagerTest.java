/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;

class ServerManagerTest {

    private static final InetSocketAddress REMOTE_ID_1 = new InetSocketAddress("10.1.0.2", 8080);
    private static final InetSocketAddress REMOTE_ID_2 = new InetSocketAddress("10.1.0.3", 8080);

    private static final Duration PING_INTERVAL = Duration.ofSeconds(5);

    @Test
    void shouldCreateNewMonitorForNewConnection() {
        // Given
        MonitorFactory monitorFactory = (_, _, _, _, _, remoteId) -> new FakeMonitor(remoteId);
        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                PING_INTERVAL
        );

        // When
        boolean result = serverManager.createNewMonitorIfNotAlreadyPresent(mock(SocketChannel.class), mock(MeshMonitor.class), REMOTE_ID_1);

        // Then
        assertThat(result).isTrue();
        assertThat(serverManager.getMonitors()).hasSize(1);
        assertThat(serverManager.getConnections()).containsOnly(REMOTE_ID_1);
        assertThat(serverManager.hasConnection(REMOTE_ID_1)).isTrue();
    }

    @Test
    void shouldNotCreateNewMonitorIfOneIsAlreadyPresent() {
        // Given
        MonitorFactory monitorFactory = (_, _, _, _, _, remoteId) -> new FakeMonitor(remoteId);
        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                PING_INTERVAL
        );

        // When
        boolean shouldBeTrue = serverManager.createNewMonitorIfNotAlreadyPresent(mock(SocketChannel.class), mock(MeshMonitor.class), REMOTE_ID_1);
        boolean shouldAlsoBeTrue = serverManager.createNewMonitorIfNotAlreadyPresent(mock(SocketChannel.class), mock(MeshMonitor.class), REMOTE_ID_2);
        boolean shouldBeFalse = serverManager.createNewMonitorIfNotAlreadyPresent(mock(SocketChannel.class), mock(MeshMonitor.class), REMOTE_ID_1);

        // Then
        assertThat(shouldBeTrue).isTrue();
        assertThat(shouldAlsoBeTrue).isTrue();
        assertThat(shouldBeFalse).isFalse();

        assertThat(serverManager.getMonitors()).hasSize(2);
        assertThat(serverManager.getConnections()).containsOnly(REMOTE_ID_1, REMOTE_ID_2);

        assertThat(serverManager.hasConnection(REMOTE_ID_1)).isTrue();
        assertThat(serverManager.hasConnection(REMOTE_ID_2)).isTrue();
    }

    @Test
    void shouldReturnOnlyRunningMonitors() {
        // Given
        MonitorFactory monitorFactory = mock(MonitorFactory.class);
        when(monitorFactory.newMonitor(any(), any(), any(), any(), any(), eq(REMOTE_ID_1))).thenReturn(new FakeMonitor(REMOTE_ID_1, true));
        when(monitorFactory.newMonitor(any(), any(), any(), any(), any(), eq(REMOTE_ID_2))).thenReturn(new FakeMonitor(REMOTE_ID_2, false));

        ServerManager serverManager = new ServerManager(
                loggerForTest(),
                monitorFactory,
                PING_INTERVAL
        );

        // When
        serverManager.createNewMonitorIfNotAlreadyPresent(mock(SocketChannel.class), mock(MeshMonitor.class), REMOTE_ID_1);
        serverManager.createNewMonitorIfNotAlreadyPresent(mock(SocketChannel.class), mock(MeshMonitor.class), REMOTE_ID_2);

        // Then
        assertThat(serverManager.getMonitors()).hasSize(1);
        assertThat(serverManager.getConnections()).containsOnly(REMOTE_ID_1);

        assertThat(serverManager.hasConnection(REMOTE_ID_1)).isTrue();
        assertThat(serverManager.hasConnection(REMOTE_ID_2)).isFalse();
    }
}
