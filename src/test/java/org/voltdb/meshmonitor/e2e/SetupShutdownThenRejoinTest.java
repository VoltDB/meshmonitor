package org.voltdb.meshmonitor.e2e;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class SetupShutdownThenRejoinTest extends ContainerTestBase {

    @Test
    void shouldSetupMeshAndWorkAfterOneNodeRestarts() {
        meshmonitor0.start();
        meshmonitor1.start();
        meshmonitor2.start();

        await("Initial mesh forming")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs0).contains("Connected to 2 servers");
                    assertThat(logs1).contains("Connected to 2 servers");
                    assertThat(logs2).contains("Connected to 2 servers");
                });

        meshmonitor1.stop();

        await("Working after one node down")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs0).contains("Connected to 1 servers");
                    assertThat(logs2).contains("Connected to 1 servers");
                });

        clearLogs();
        meshmonitor1.start();

        await("Working after node reconnects")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs0).contains("Connected to 2 servers");
                    assertThat(logs1).contains("Connected to 2 servers");
                    assertThat(logs2).contains("Connected to 2 servers");
                });
    }
}
