package org.voltdb.meshmonitor.metrics;

import org.HdrHistogram.SynchronizedHistogram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.voltdb.meshmonitor.ConsoleLoggerTest;
import org.voltdb.meshmonitor.MeshMonitorTimings;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class HistogramPrinterTest {

    private static final InetSocketAddress REMOTE_ID = new InetSocketAddress("remote.host.com", 8080);
    private SynchronizedHistogram histogram;

    @BeforeEach
    void setUp() {
        histogram = MeshMonitorTimings.createDefault(ConsoleLoggerTest.loggerForTest())
                .deltaHistogram()
                .getCumulativeHistogram();
    }

    @Test
    public void shouldPrintBasicHistogram() {
        // Given
        histogram.recordValue(42L);

        HistogramPrinter printer = new HistogramPrinter("host_name");
        StringBuilder actual = new StringBuilder();

        // When
        printer.printHistogram(actual, histogram, REMOTE_ID, "empty_histogram");

        // Then
        String result = actual.toString();
        assertThat(result).contains("meshmonitor_empty_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.000010\"} 0");
        assertThat(result).contains("meshmonitor_empty_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.000100\"} 1");
        assertThat(result).contains("meshmonitor_empty_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"+Inf\"} 1");
        assertThat(result).contains("meshmonitor_empty_histogram_sum{host_name=\"host_name\",remote_host_name=\"remote_host_com\",} 42");
        assertThat(result).contains("meshmonitor_empty_histogram_count{host_name=\"host_name\",remote_host_name=\"remote_host_com\",} 1");
    }

    @Test
    public void shouldFormatCumulativeBuckets() {
        // Given
        histogram.recordValue(42L);
        histogram.recordValue(420L);
        histogram.recordValue(4200L);

        HistogramPrinter printer = new HistogramPrinter("host_name");
        StringBuilder actual = new StringBuilder();

        // When
        printer.printHistogram(actual, histogram, REMOTE_ID, "histogram");

        // Then
        String result = actual.toString();
        assertThat(result).contains("meshmonitor_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.000010\"} 0");
        assertThat(result).contains("meshmonitor_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.000100\"} 1");
        assertThat(result).contains("meshmonitor_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.000500\"} 2");
        assertThat(result).contains("meshmonitor_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.001000\"} 2");
        assertThat(result).contains("meshmonitor_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"0.005000\"} 3");
        assertThat(result).contains("meshmonitor_histogram_bucket{host_name=\"host_name\",remote_host_name=\"remote_host_com\",le=\"+Inf\"} 3");
        assertThat(result).contains("meshmonitor_histogram_sum{host_name=\"host_name\",remote_host_name=\"remote_host_com\",} 4665");
        assertThat(result).contains("meshmonitor_histogram_count{host_name=\"host_name\",remote_host_name=\"remote_host_com\",} 3");
    }
}
