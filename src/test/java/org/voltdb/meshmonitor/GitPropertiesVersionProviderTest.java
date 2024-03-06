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
                        """
                                git.tags=v1.0
                                git.build.time=2024-01-01
                                git.commit.id.abbrev=abc123
                                """,
                        "version v1.0 built on 2024-01-01"
                ),
                Arguments.of(
                        """
                                git.tags=
                                git.build.time=2024-01-01
                                git.commit.id.abbrev=abc123
                                """,
                        "version abc123 built on 2024-01-01"
                ),
                Arguments.of(
                        """
                                git.tags=
                                git.build.time=2024-01-01
                                git.commit.id.abbrev=
                                """,
                        "version unknown built on 2024-01-01"
                ),
                Arguments.of(
                        """
                                git.tags=
                                git.build.time=
                                git.commit.id.abbrev=
                                """,
                        "version unknown"
                ),
                Arguments.of(
                        """
                                git.build.time=2024-01-01
                                git.commit.id.abbrev=abc123
                                """,
                        "version abc123 built on 2024-01-01"
                ),
                Arguments.of(
                        """
                                git.build.time=2024-01-01
                                """,
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
