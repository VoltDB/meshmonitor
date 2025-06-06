/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.voltdb.meshmonitor.ConsoleLogger;
import org.voltdb.meshmonitor.ServerManager;

import java.io.IOException;
import java.io.OutputStream;

class MetricsHttpHandler implements HttpHandler {

    private static final String SUPPORTED_HTTP_METHOD = "GET";
    private static final String PATH = "/metrics";

    private final ConsoleLogger logger;
    private final MonitorStatsPrinter monitorStatsPrinter;
    private final ServerManager serverManager;

    public MetricsHttpHandler(ConsoleLogger logger, String hostName, ServerManager serverManager) {
        this.logger = logger;
        this.monitorStatsPrinter = new MonitorStatsPrinter(hostName);
        this.serverManager = serverManager;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (!SUPPORTED_HTTP_METHOD.equals(httpExchange.getRequestMethod())) {
            replyWith404(httpExchange);
            return;
        }

        String path = httpExchange.getRequestURI().getPath();
        if (PATH.equals(path)) {
            handleGetRequest(httpExchange);
        } else {
            replyWith404(httpExchange);
        }
    }

    private void replyWith404(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(404, 0);
    }

    private void handleGetRequest(HttpExchange httpExchange) throws IOException {
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            StringBuilder output = new StringBuilder();
            serverManager.getMonitors().forEach(monitor -> monitorStatsPrinter.print(output, monitor));
            String prometheusResponse = output.toString();

            // Set Content-Type header (required by Prometheus)
            // see: https://prometheus.io/docs/instrumenting/exposition_formats/#text-format-details
            httpExchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");

            httpExchange.sendResponseHeaders(200, prometheusResponse.length());

            outputStream.write(prometheusResponse.getBytes());
        } catch (Exception e) {
            logger.log("Error while generating Prometheus response", e);
        }
    }
}
