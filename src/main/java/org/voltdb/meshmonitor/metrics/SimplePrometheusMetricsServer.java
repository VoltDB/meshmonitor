/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import com.sun.net.httpserver.HttpServer;
import org.voltdb.meshmonitor.ConsoleLogger;
import org.voltdb.meshmonitor.ServerManager;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class SimplePrometheusMetricsServer {

    private final ConsoleLogger logger;

    private final InetSocketAddress bindAddress;
    private final String reportedHostName;

    private final ServerManager serverManager;
    private HttpServer server;

    public SimplePrometheusMetricsServer(
            ConsoleLogger logger,
            InetSocketAddress bindAddress,
            String reportedHostName,
            ServerManager serverManager) {
        this.logger = logger;
        this.bindAddress = bindAddress;
        this.reportedHostName = reportedHostName;
        this.serverManager = serverManager;
    }

    public void start() throws IOException {
        try {
            server = HttpServer.create(bindAddress, 100);
        } catch (BindException e) {
            throw new IOException("Address " + bindAddress + " already in use", e);
        }

        server.createContext(
                "/metrics",
                new MetricsHttpHandler(logger, reportedHostName, serverManager));
        server.start();

        logger.log("Prometheus endpoint available at http://%s/metrics", server.getAddress());
    }

    public void close() {
        server.stop(0);
    }
}
