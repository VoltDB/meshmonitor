package org.voltdb.meshmonitor.e2e;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class SurviveMainNodeRestartTest extends ContainerTestBase {

    @Test
    void shouldSetupMeshAndWorkAfterMainNodeRestarts() {
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

        meshmonitor0.stop();

        await("Working after main node down")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs1).contains("establishing connection");
                    assertThat(logs2).contains("establishing connection");
                });

        clearLogs();
        meshmonitor0.start();

        await("Working after node starts up")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs0).contains("Connected to 2 servers");
                    assertThat(logs1).contains("Connected to 2 servers");
                    assertThat(logs2).contains("Connected to 2 servers");
                });
    }
}
