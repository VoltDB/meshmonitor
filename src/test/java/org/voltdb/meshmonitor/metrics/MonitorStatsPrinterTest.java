package org.voltdb.meshmonitor.metrics;

import org.junit.jupiter.api.Test;
import org.voltdb.meshmonitor.ConsoleLogger;
import org.voltdb.meshmonitor.MeshMonitorTimings;
import org.voltdb.meshmonitor.Monitor;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitorStatsPrinterTest {

    private static final InetSocketAddress REMOTE_ID = new InetSocketAddress("remote.host.com", 8080);

    @Test
    void shouldPrintOutputOfAllHistograms() {
        // Given
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(new ConsoleLogger(false));
        timings.sendHistogram().recordValueWithExpectedInterval(5, 5);
        timings.deltaHistogram().recordValueWithExpectedInterval(15, 15);
        timings.receiveHistogram().recordValueWithExpectedInterval(42, 42);

        MonitorStatsPrinter printer = new MonitorStatsPrinter("host");

        Monitor monitor = mock(Monitor.class);
        when(monitor.getTimings()).thenReturn(timings);
        when(monitor.getRemoteId()).thenReturn(REMOTE_ID);

        StringBuilder actual = new StringBuilder();

        // When
        printer.print(actual, monitor);

        // Then
        assertThat(actual)
                .contains("meshmonitor_receive_seconds_bucket{host_name=\"host\",remote_host_name=\"remote_host_com\",le=\"0.000010\"} 0")
                .contains("meshmonitor_receive_seconds_bucket{host_name=\"host\",remote_host_name=\"remote_host_com\",le=\"0.000100\"} 1")
                .contains("meshmonitor_receive_seconds_sum{host_name=\"host\",remote_host_name=\"remote_host_com\",} 42")
                .contains("meshmonitor_delta_seconds_bucket{host_name=\"host\",remote_host_name=\"remote_host_com\",le=\"0.000010\"} 0")
                .contains("meshmonitor_delta_seconds_bucket{host_name=\"host\",remote_host_name=\"remote_host_com\",le=\"0.000100\"} 1")
                .contains("meshmonitor_delta_seconds_sum{host_name=\"host\",remote_host_name=\"remote_host_com\",} 15")
                .contains("meshmonitor_send_seconds_bucket{host_name=\"host\",remote_host_name=\"remote_host_com\",le=\"0.000010\"} 1")
                .contains("meshmonitor_send_seconds_sum{host_name=\"host\",remote_host_name=\"remote_host_com\",} 5");
    }
}
