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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import org.voltdb.meshmonitor.ConsoleLogger;
import org.voltdb.meshmonitor.ServerManager;

import com.sun.net.httpserver.HttpServer;

public class SimplePrometheusMetricsServer {

    private final ConsoleLogger logger;
    private final String hostName;
    private final InetSocketAddress bindAddress;
    private final ServerManager serverManager;
    private HttpServer server;

    public SimplePrometheusMetricsServer(
                                         ConsoleLogger logger,
                                         String hostName,
                                         InetSocketAddress bindAddress,
                                         ServerManager serverManager) {
        this.logger = logger;
        this.hostName = hostName;
        this.bindAddress = bindAddress;
        this.serverManager = serverManager;
    }

    public void start() {
        try {
            server = HttpServer.create(bindAddress, 100);
            server.createContext(
                    "/metrics",
                    new MetricsHttpHandler(hostName, serverManager));
            server.start();

            logger.log("HTTP server listening on http://%s/metrics", server.getAddress());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void close() {
        server.stop(0);
    }
}
