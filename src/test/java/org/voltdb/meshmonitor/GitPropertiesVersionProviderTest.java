/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GitPropertiesVersionProviderTest {

    static Stream<Arguments> variousPropertiesContents() {
        return Stream.of(
                Arguments.of(
                        "git.tags=v1.0\n" +
                        "git.build.time=2024-01-01\n" +
                        "git.commit.id.abbrev=abc123\n",
                        "version v1.0 built on 2024-01-01"
                ),
                Arguments.of(
                        "git.tags=\n" +
                        "git.build.time=2024-01-01\n" +
                        "git.commit.id.abbrev=abc123\n",
                        "version abc123 built on 2024-01-01"
                ),
                Arguments.of(
                        "git.tags=\n" +
                        "git.build.time=2024-01-01\n" +
                        "git.commit.id.abbrev=\n",
                        "version unknown built on 2024-01-01"
                ),
                Arguments.of(
                        "git.tags=\n" +
                        "git.build.time=\n" +
                        "git.commit.id.abbrev=\n",
                        "version unknown"
                ),
                Arguments.of(
                        "git.build.time=2024-01-01\n" +
                        "git.commit.id.abbrev=abc123\n",
                        "version abc123 built on 2024-01-01"
                ),
                Arguments.of(
                        "git.build.time=2024-01-01\n",
                        "version unknown built on 2024-01-01"
                ),
                Arguments.of(
                        "",
                        "version unknown"
                )
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("variousPropertiesContents")
    public void test(String propertiesContent, String expectedLine) {
        ByteArrayInputStream input = new ByteArrayInputStream(propertiesContent.getBytes());
        GitPropertiesVersionProvider provider = new GitPropertiesVersionProvider(() -> input);

        // When
        String[] version = provider.getVersion();

        // THen
        assertThat(version).hasSize(1);
        assertThat(version[0])
                .startsWith(GitPropertiesVersionProvider.VERSION_PREFIX)
                .contains(expectedLine);
    }

    @Test
    public void shouldHandleExceptionWhenRetrievingVersion() {
        // Given
        GitPropertiesVersionProvider provider = new GitPropertiesVersionProvider(() -> {
            throw new NullPointerException();
        });

        // When
        String[] version = provider.getVersion();

        // THen
        assertThat(version).hasSize(1);
        assertThat(version[0])
                .startsWith(GitPropertiesVersionProvider.VERSION_PREFIX)
                .contains("unknown");
    }
}
