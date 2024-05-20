/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.voltdb.meshmonitor.ConsoleLoggerTest;
import org.voltdb.meshmonitor.MeshMonitorTimings;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class HistogramPrinterTest {

    private static final InetSocketAddress REMOTE_ID = new InetSocketAddress("remote.host.com", 8080);

    private MeshMonitorTimings meshMonitorTimings;

    @BeforeEach
    void setUp() {
        meshMonitorTimings = MeshMonitorTimings.createDefault(ConsoleLoggerTest.loggerForTest());
    }

    @Test
    public void shouldPrintBasicHistogram() {
        // Given
        meshMonitorTimings
                .deltaHistogram()
                .recordValueWithExpectedInterval(42L, 42L);

        HistogramPrinter printer = new HistogramPrinter("host_name");
        StringBuilder actual = new StringBuilder();

        // When
        meshMonitorTimings
                .deltaHistogram()
                .getCumulativeHistogram(histogram ->
                        printer.printHistogram(actual, histogram, REMOTE_ID, "empty_histogram")
                );

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
        meshMonitorTimings
                .deltaHistogram()
                .recordValueWithExpectedInterval(42L, 42L);

        meshMonitorTimings
                .deltaHistogram()
                .recordValueWithExpectedInterval(420L, 420L);

        meshMonitorTimings
                .deltaHistogram()
                .recordValueWithExpectedInterval(4200L, 4200L);

        HistogramPrinter printer = new HistogramPrinter("host_name");
        StringBuilder actual = new StringBuilder();

        // When
        meshMonitorTimings
                .deltaHistogram()
                .getCumulativeHistogram(histogram ->
                        printer.printHistogram(actual, histogram, REMOTE_ID, "histogram")
                );

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
