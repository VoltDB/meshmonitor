/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.e2e;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class ShouldFormMeshFromDisjointSetsOfServersIT extends ContainerTestBase {

    @Test
    void shouldSetupMeshAndWorkEvenWhenMainNodeStartsLast() {
        meshmonitor0.withCommand("/home/meshmonitor/meshmonitor",
                "-m",
                "192.168.0.2:12223",
                "-b",
                "192.168.0.2:12222"
        ).start();

        meshmonitor1.withCommand("/home/meshmonitor/meshmonitor",
                "-m",
                "192.168.0.3:12223",
                "-b",
                "192.168.0.3:12222",
                "192.168.0.2"
        ).start();

        meshmonitor2.withCommand("/home/meshmonitor/meshmonitor",
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
