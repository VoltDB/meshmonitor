/* This file is part of VoltDB.
 * Copyright (C) 2023 Volt Active Data Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltdb.meshmonitor.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Metric;
import io.prometheus.metrics.core.metrics.MetricWithFixedMetadata;
import io.prometheus.metrics.model.snapshots.Unit;

import java.util.function.Function;

import static org.voltdb.meshmonitor.metrics.MeshmonitorMetrics.COUNT;

@SuppressWarnings("rawtypes")
public enum MeshmonitorMetric {

    PROCEDURE_INVOCATIONS("procedure_invocation_completed",
            COUNT,
            ClientStats::getInvocationsCompleted,
            "connection_id", "procedure"),
    PROCEDURE_ERROR("procedure_invocation_errors",
            COUNT,
            ClientStats::getInvocationErrors,
            "connection_id", "procedure"),
    PROCEDURE_ABORTS("procedure_invocation_aborts",
            COUNT,
            ClientStats::getInvocationAborts,
            "connection_id", "procedure"),
    PROCEDURE_TIMEOUTS("procedure_invocation_timeouts",
            COUNT,
            ClientStats::getInvocationTimeouts,
            "connection_id", "procedure"),
    PROCEDURE_LATENCY("procedure_invocation_latency_avg",
            Unit.SECONDS,
            ClientStats::getAverageLatency,
            "connection_id", "procedure") {
        @Override
        public MetricWithFixedMetadata.Builder newMetric() {
            return Histogram.builder();
        }
    },
    PROCEDURE_LATENCY_INTERNAL("procedure_invocation_internal_latency_avg",
            Unit.SECONDS,
            ClientStats::getAverageInternalLatency,
            "connection_id", "procedure"),

    PROCEDURE_THROUGHPUT("procedure_invocation_throughput",
            COUNT,
            ClientStats::getTxnThroughput,
            "connection_id", "procedure"),

    PROCEDURE_BYTES_READ("procedure_bytes_read",
            Unit.BYTES,
            ClientStats::getBytesRead,
            "connection_id", "procedure"),

    PROCEDURE_BYTES_WRITTEN("procedure_bytes_written",
            Unit.BYTES,
            ClientStats::getBytesWritten,
            "connection_id", "procedure"),

    PROCEDURE_IO_READ("procedure_io_read_throughput",
            Unit.BYTES,
            ClientStats::getIOReadThroughput,
            "connection_id", "procedure"),

    PROCEDURE_IO_WRITE("procedure_io_write_throughput",
            Unit.BYTES,
            ClientStats::getIOWriteThroughput,
            "connection_id", "procedure"),

    AFFINITY_READS("affinity_reads", COUNT, null, "connection_id") {
        @Override
        public void update(ClientStatsContext context, Metric metric) {
            context.getAffinityStats().forEach((connectionId, stats) -> {
                ((Counter) metric).labelValues(String.valueOf(connectionId))
                        .inc(stats.getAffinityReads());
            });
        }
    },

    AFFINITY_WRITES("affinity_writes", COUNT, null, "connection_id") {
        @Override
        public void update(ClientStatsContext context, Metric metric) {
            context.getAffinityStats().forEach((connectionId, stats) -> {
                ((Counter) metric).labelValues(String.valueOf(connectionId))
                        .inc(stats.getAffinityWrites());
            });
        }
    },

    AFFINITY_RR_READS("affinity_reads_rr", COUNT, null, "connection_id") {
        @Override
        public void update(ClientStatsContext context, Metric metric) {
            context.getAffinityStats().forEach((connectionId, stats) -> {
                ((Counter) metric).labelValues(String.valueOf(connectionId))
                        .inc(stats.getRrReads());
            });
        }
    },

    AFFINITY_RR_WRITES("affinity_writes_rr", COUNT, null, "connection_id") {
        @Override
        public void update(ClientStatsContext context, Metric metric) {
            context.getAffinityStats().forEach((connectionId, stats) -> {
                ((Counter) metric).labelValues(String.valueOf(connectionId))
                        .inc(stats.getRrWrites());
            });
        }
    };

    private final String metricName;
    private final Unit unit;
    private final String[] labelNames;

    private final Function<ClientStats, Number> statsExtractor;

    MeshmonitorMetric(String metricName, Unit unit, Function<ClientStats, Number> statsExtractor, String... labelNames) {
        this.metricName = metricName;
        this.unit = unit;
        this.labelNames = labelNames;

        this.statsExtractor = statsExtractor;
    }

    public String getMetricName() {
        return metricName;
    }

    public Unit getUnit() {
        return unit;
    }

    public String[] getLabelNames() {
        return labelNames;
    }

    public void update(ClientStatsContext context, Metric metric) {
        context.getCompleteStats()
                .forEach((connectionId, statsByProcedure) -> statsByProcedure
                        .forEach((procedure, stats) -> {
                                    String connectionName = String.valueOf(connectionId);
                                    String procedureName = procedure.replaceAll("@", "");
                                    double value = statsExtractor.apply(stats).doubleValue();

                                    updateMetric(metric, connectionName, procedureName, value);
                                }
                        ));
    }

    private void updateMetric(Metric metric, String connectionName, String procedureName, double value) {
        if (metric instanceof Counter) {
            Counter counter = (Counter) metric;
            counter.labelValues(connectionName, procedureName)
                    .inc(value);
        } else if (metric instanceof Histogram) {
            Histogram histogram = (Histogram) metric;
            histogram.labelValues(connectionName, procedureName)
                    .observe(value);
        }
    }

    public MetricWithFixedMetadata.Builder newMetric() {
        return Counter.builder();
    }
}
