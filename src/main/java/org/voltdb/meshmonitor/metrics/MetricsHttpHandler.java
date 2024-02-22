package org.voltdb.meshmonitor.metrics;

import java.io.IOException;
import java.io.OutputStream;

import org.voltdb.meshmonitor.ServerManager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class MetricsHttpHandler implements HttpHandler {

    private static final String SUPPORTED_HTTP_METHOD = "GET";
    private static final String PATH = "/metrics";

    private final MonitorStatsPrinter monitorStatsPrinter;
    private final ServerManager serverManager;

    public MetricsHttpHandler(String hostName, ServerManager serverManager) {
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
        }
        else {
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
            httpExchange.sendResponseHeaders(200, prometheusResponse.length());

            outputStream.write(prometheusResponse.getBytes());
        }
    }
}
