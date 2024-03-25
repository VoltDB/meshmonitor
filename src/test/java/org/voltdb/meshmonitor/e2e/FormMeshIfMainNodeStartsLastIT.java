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

public class FormMeshIfMainNodeStartsLastIT extends ContainerTestBase {

    @Test
    void shouldSetupMeshAndWorkEvenWhenMainNodeStartsLast() {
        meshmonitor1.start();
        meshmonitor2.start();

        await("Initial mesh forming")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs1).contains("Connected to 0 servers");
                    assertThat(logs2).contains("Connected to 0 servers");
                });

        meshmonitor0.start();

        await("Mesh formation after main node starts up")
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> {
                    assertThat(logs0).contains("Connected to 2 servers");
                    assertThat(logs1).contains("Connected to 2 servers");
                    assertThat(logs2).contains("Connected to 2 servers");
                });
    }
}
