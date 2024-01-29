///* This file is part of VoltDB.
// * Copyright (C) 2023 Volt Active Data Inc.
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as
// * published by the Free Software Foundation, either version 3 of the
// * License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.voltdb.meshmonitor.metrics;
//
//import io.prometheus.metrics.core.metrics.Metric;
//import io.prometheus.metrics.model.snapshots.Labels;
//import io.prometheus.metrics.model.snapshots.Unit;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class MeshmonitorMetrics implements AutoCloseable {
//
//    public static final Unit COUNT = new Unit("count");
//
//    private final Map<MeshmonitorMetric, Metric> metricUpdaters;
//    private final MeshMonitorMetricsServer server;
//
//    public MeshmonitorMetrics(String application, int port) {
//        metricUpdaters = new HashMap<>();
//        server = MeshMonitorMetricsServer.withPort(port);
//
//        for (MeshmonitorMetric meshmonitorMetric : MeshmonitorMetric.values()) {
//            Metric metric = meshmonitorMetric.newMetric()
//                    .name(meshmonitorMetric.getMetricName())
//                    .constLabels(Labels.of(
//                                    "application", application,
//                                    "host_name", MeshMonitorMetricsServer.getLocalHost()
//                            )
//                    )
//                    .labelNames(meshmonitorMetric.getLabelNames())
//                    .unit(meshmonitorMetric.getUnit())
//                    .register();
//
//            metricUpdaters.put(meshmonitorMetric, metric);
//        }
//    }
//
////    public void tick(ClientStatsContext context) {
////        metricUpdaters.forEach((clientMetric, prometheusMetric) -> clientMetric.update(context, prometheusMetric));
////    }
//
//    public void start() {
//        server.start();
//    }
//
//    @Override
//    public void close() {
//        server.close();
//    }
//}
