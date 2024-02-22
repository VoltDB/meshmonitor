package org.voltdb.meshmonitor.metrics;

import com.sun.net.httpserver.HttpServer;
import org.voltdb.meshmonitor.ConsoleLogger;
import org.voltdb.meshmonitor.ServerManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

public class SimplePrometheusMetricsServer {

    private final ConsoleLogger logger;
    private final InetSocketAddress bindAddress;
    private final ServerManager serverManager;
    private HttpServer server;

    public SimplePrometheusMetricsServer(
            ConsoleLogger logger,
            InetSocketAddress bindAddress,
            ServerManager serverManager) {
        this.logger = logger;
        this.bindAddress = bindAddress;
        this.serverManager = serverManager;
    }

    public void start() {
        try {
            server = HttpServer.create(bindAddress, 100);
            server.createContext(
                    "/metrics",
                    new MetricsHttpHandler(bindAddress.getHostName(), serverManager));
            server.start();

            logger.log("HTTP server listening on http://%s/metrics", server.getAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void close() {
        server.stop(0);
    }
}
