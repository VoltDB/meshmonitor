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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void close() {
        server.stop(0);
    }
}
