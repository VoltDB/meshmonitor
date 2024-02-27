/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import java.net.InetSocketAddress;

import org.HdrHistogram.Histogram;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class HistogramPrinter {

    private static final long inf = SECONDS.toMicros(10);

    private static final long[] BUCKETS_MICROSECONDS = new long[]{
            10,
            100,
            500,
            MILLISECONDS.toMicros(1),
            MILLISECONDS.toMicros(2),
            MILLISECONDS.toMicros(3),
            MILLISECONDS.toMicros(4),
            MILLISECONDS.toMicros(5),
            MILLISECONDS.toMicros(6),
            MILLISECONDS.toMicros(7),
            MILLISECONDS.toMicros(8),
            MILLISECONDS.toMicros(9),
            MILLISECONDS.toMicros(10),
            MILLISECONDS.toMicros(20),
            MILLISECONDS.toMicros(30),
            MILLISECONDS.toMicros(40),
            MILLISECONDS.toMicros(50),
            MILLISECONDS.toMicros(100),
            MILLISECONDS.toMicros(200),
            MILLISECONDS.toMicros(500),
            SECONDS.toMicros(1),
            SECONDS.toMicros(2),
            SECONDS.toMicros(5),
            SECONDS.toMicros(10),
            inf
    };

    private final String hostNameLabel;

    public HistogramPrinter(String hostName) {
        hostNameLabel = String.format("host_name=\"%s\",", hostName).replace('.', '_');
    }

    public void printHistogram(StringBuilder output, Histogram histogram, InetSocketAddress remoteId, String metricName) {
        String remoteHostNameLabel = String.format(
                "remote_host_name=\"%s\",",
                remoteId.getAddress().getHostAddress())
                .replace('.', '_');

        output.append("# TYPE meshmonitor_")
                .append(metricName)
                .append(" histogram\n");

        long runningCount = 0L;
        long bucketStart = 0L;
        for (int i = 0; i < BUCKETS_MICROSECONDS.length - 1; i++) {
            long start = bucketStart;
            long end = histogram.highestEquivalentValue(BUCKETS_MICROSECONDS[i]);

            long value = histogram.getCountBetweenValues(start, end);
            runningCount += value;
            String bucketLe = String.format("%f", BUCKETS_MICROSECONDS[i] / 1000.0 / 1000.0);
            printBucket(output, remoteHostNameLabel, metricName, bucketLe, runningCount);
            bucketStart = end + 1;
        }

        int lastElementIndex = BUCKETS_MICROSECONDS.length - 1;
        long end = histogram.highestEquivalentValue(BUCKETS_MICROSECONDS[lastElementIndex]);
        long start = histogram.highestEquivalentValue(BUCKETS_MICROSECONDS[lastElementIndex - 1]) + 1;
        long value = histogram.getCountBetweenValues(start, end);
        runningCount += value;

        printBucket(output, remoteHostNameLabel, metricName, "+Inf", runningCount);
        printSum(output, remoteHostNameLabel, metricName, histogram.getTotalCount());
        printCount(output, remoteHostNameLabel, metricName, runningCount);
    }

    private void printSum(StringBuilder output, String remoteHostNameLabel, String metricName, long value) {
        output.append("meshmonitor_")
                .append(metricName)
                .append("_sum")
                .append("{")
                .append(hostNameLabel)
                .append(remoteHostNameLabel)
                .append("} ")
                .append(value)
                .append('\n');
    }

    private void printCount(StringBuilder output, String remoteHostNameLabel, String metricName, long value) {
        output.append("meshmonitor_")
                .append(metricName)
                .append("_count")
                .append("{")
                .append(hostNameLabel)
                .append(remoteHostNameLabel)
                .append("} ")
                .append(value)
                .append('\n');
    }

    private void printBucket(StringBuilder output,
                             String remoteHostNameLabel,
                             String metricName,
                             String bucketStart,
                             long runningCount) {
        output.append("meshmonitor_")
                .append(metricName)
                .append("_bucket")
                .append("{")
                .append(hostNameLabel)
                .append(remoteHostNameLabel);

        output.append("le=\"")
                .append(bucketStart)
                .append("\"")
                .append("} ")
                .append(runningCount)
                .append('\n');
    }
}
