/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import io.restassured.RestAssured;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.voltdb.meshmonitor.*;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimplePrometheusMetricsServerTest {

    private static final int EXPECTED_ERROR_CODE = 404;

    private static final ConsoleLogger LOGGER = ConsoleLoggerTest.loggerForTest();

    private static SimplePrometheusMetricsServer server;

    private static ServerManager serverManager;

    @BeforeAll
    static void setUp() {
        InetSocketAddress address = MonitorTest.address("0.0.0.0");

        serverManager = mock(ServerManager.class);
        server = new SimplePrometheusMetricsServer(
                LOGGER,
                address,
                serverManager);
        server.start();

        RestAssured.port = address.getPort();
    }

    @AfterAll
    static void tearDown() {
        server.close();
    }

    @Test
    void shouldReturnErrorOnInvalidHttpMethod() {
        given()
                .when()
                .post("/metrics")
                .then()
                .statusCode(EXPECTED_ERROR_CODE);
    }

    @Test
    void shouldReturnErrorOnInvalidUrl() {
        given()
                .when()
                .get("/metricss")
                .then()
                .statusCode(EXPECTED_ERROR_CODE);

        given()
                .when()
                .get("/")
                .then()
                .statusCode(EXPECTED_ERROR_CODE);

        given()
                .when()
                .get()
                .then()
                .statusCode(EXPECTED_ERROR_CODE);
    }

    @Test
    void shouldRespondWithMetricsWhenThereAreNoMonitors() {
        when(serverManager.getMonitors()).thenReturn(List.of());

        given()
                .when()
                .get("/metrics")
                .then()
                .statusCode(200)
                .body(is(emptyString()));
    }

    @Test
    void shouldRespondWithMetricsFomOneMonitor() {
        Monitor monitor = createMonitorWithTimings("remote.host.com", 5_000);
        when(serverManager.getMonitors()).thenReturn(List.of(monitor));

        given()
                .when()
                .get("/metrics")
                .then()
                .statusCode(200)
                .body(
                        containsString("meshmonitor_receive_seconds_bucket{host_name=\"0_0_0_0\",remote_host_name=\"remote_host_com\",le=\"0.005000\"} 1"),
                        containsString("meshmonitor_delta_seconds_sum{host_name=\"0_0_0_0\",remote_host_name=\"remote_host_com\",} 0"),
                        containsString("meshmonitor_send_seconds_sum{host_name=\"0_0_0_0\",remote_host_name=\"remote_host_com\",} 0")
                );
    }

    @Test
    void shouldRespondWithMetricsFromManyMonitors() {
        Monitor monitor1 = createMonitorWithTimings("remote.host.com", 5_000);
        Monitor monitor2 = createMonitorWithTimings("other.host.com", 42_000);

        when(serverManager.getMonitors()).thenReturn(List.of(monitor1, monitor2));

        given()
                .when()
                .get("/metrics")
                .then()
                .statusCode(200)
                .body(
                        containsString("meshmonitor_receive_seconds_bucket{host_name=\"0_0_0_0\",remote_host_name=\"remote_host_com\",le=\"0.005000\"} 1"),
                        containsString("meshmonitor_delta_seconds_sum{host_name=\"0_0_0_0\",remote_host_name=\"remote_host_com\",} 0"),
                        containsString("meshmonitor_send_seconds_sum{host_name=\"0_0_0_0\",remote_host_name=\"remote_host_com\",} 0"),

                        containsString("meshmonitor_receive_seconds_bucket{host_name=\"0_0_0_0\",remote_host_name=\"other_host_com\",le=\"0.005000\"} 0"),
                        containsString("meshmonitor_receive_seconds_bucket{host_name=\"0_0_0_0\",remote_host_name=\"other_host_com\",le=\"0.050000\"} 1"),
                        containsString("meshmonitor_delta_seconds_sum{host_name=\"0_0_0_0\",remote_host_name=\"other_host_com\",} 0"),
                        containsString("meshmonitor_send_seconds_sum{host_name=\"0_0_0_0\",remote_host_name=\"other_host_com\",} 0")
                );
    }

    @NotNull
    private static Monitor createMonitorWithTimings(String host, long... values) {
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(LOGGER);
        Arrays.stream(values).forEach(value -> {
            timings.receiveHistogram()
                    // make it simple to reason for assertions so expectedIntervalBetweenValueSamples is the same as value.
                    .recordValueWithExpectedInterval(value, value);
        });

        Monitor monitor1 = mock(Monitor.class);
        when(monitor1.getTimings()).thenReturn(timings);
        when(monitor1.getRemoteId()).thenReturn(new InetSocketAddress(host, 8080));
        return monitor1;
    }
}
