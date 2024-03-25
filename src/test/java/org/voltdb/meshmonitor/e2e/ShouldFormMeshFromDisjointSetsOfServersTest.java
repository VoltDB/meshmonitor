package org.voltdb.meshmonitor.e2e;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class ShouldFormMeshFromDisjointSetsOfServersTest extends ContainerTestBase {

    @Test
    void shouldSetupMeshAndWorkEvenWhenMainNodeStartsLast() {
        meshmonitor0.withCommand("java",
                "--enable-preview",
                "-cp",
                "/home/meshmonitor/target/meshmonitor-1.0.0-jar-with-dependencies.jar",
                "org.voltdb.meshmonitor.cli.MeshMonitorCommand",
                "-i",
                "1",
                "-m",
                "192.168.0.2:12223",
                "-b",
                "192.168.0.2:12222"
        ).start();

        meshmonitor1.withCommand("java",
                "--enable-preview",
                "-cp",
                "/home/meshmonitor/target/meshmonitor-1.0.0-jar-with-dependencies.jar",
                "org.voltdb.meshmonitor.cli.MeshMonitorCommand",
                "-i",
                "1",
                "-m",
                "192.168.0.3:12223",
                "-b",
                "192.168.0.3:12222",
                "192.168.0.2"
        ).start();

        meshmonitor2.withCommand("java",
                "--enable-preview",
                "-cp",
                "/home/meshmonitor/target/meshmonitor-1.0.0-jar-with-dependencies.jar",
                "org.voltdb.meshmonitor.cli.MeshMonitorCommand",
                "-i",
                "1",
                "-m",
                "192.168.0.4:12223",
                "-b",
                "192.168.0.4:12222",
                "192.168.0.3"
        ).start();

        await("Mesh formation after main node starts up")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs0).contains("Connected to 2 servers");
                    assertThat(logs1).contains("Connected to 2 servers");
                    assertThat(logs2).contains("Connected to 2 servers");
                });
    }
}
