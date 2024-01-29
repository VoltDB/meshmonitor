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

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class MeshMonitorMetricsServer implements AutoCloseable {

    private final int port;
    private HTTPServer server;

    private MeshMonitorMetricsServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            JvmMetrics.builder().register();

            server = HTTPServer.builder()
                    .port(port)
                    .buildAndStart();

            System.out.printf("HTTPServer listening on port http://localhost:%d/metrics", server.getPort());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        server.close();
    }

    public static MeshMonitorMetricsServer withPort(int port) {
        return new MeshMonitorMetricsServer(port);
    }

    static String getLocalHost() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
