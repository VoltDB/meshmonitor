/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.cli;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MeshMonitorCommandTest {

    static Stream<Arguments> invalidArguments() {
        return Stream.of(
                Arguments.of(
                        "-i=0",
                        "Invalid argument: Reporting interval must be greater than zero."
                ),
                Arguments.of(
                        "-i=-42",
                        "Invalid argument: Reporting interval must be greater than zero."
                ),
                Arguments.of(
                        "-t=0",
                        "Invalid argument: Minimum latency to report should be greater than zero."
                ),
                Arguments.of(
                        "-t=-42",
                        "Invalid argument: Minimum latency to report should be greater than zero."
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidArguments")
    void shouldCheckInvalidArguments(String argument, String errorMessage) {
        // Given
        MeshMonitorCommand app = new MeshMonitorCommand();
        CommandLine cmd = new CommandLine(app);

        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        // When
        int exitCode = cmd.execute(argument);

        // Then
        assertThat(exitCode).isEqualTo(2);
        assertThat(sw.toString()).contains(errorMessage);
    }
}
