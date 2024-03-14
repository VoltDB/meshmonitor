/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.serdes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.voltdb.meshmonitor.testutils.FakeWritableByteChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.voltdb.meshmonitor.testutils.FileChannelUtils.newReadableChannel;
import static org.voltdb.meshmonitor.testutils.FileChannelUtils.newWritableChannel;

class PacketSerializerTest {

    static Stream<Arguments> pingTestCases() {
        return Stream.of(
                Arguments.of("Ping message with empty IP list",
                        false,
                        List.of()
                ),
                Arguments.of("Ping message with empty IP list (slow connection)",
                        true,
                        List.of()
                ),
                Arguments.of("Ping message with single IP",
                        false,
                        List.of(new InetSocketAddress("127.0.0.1", 8080))
                ),
                Arguments.of("Ping message with single IP (slow connection)",
                        true,
                        List.of(new InetSocketAddress("127.0.0.1", 8080))
                ),
                Arguments.of("Ping message with many IPs",
                        false,
                        List.of(new InetSocketAddress("2001:db8::1", 8081),
                                new InetSocketAddress("127.0.0.1", 8082),
                                new InetSocketAddress("10.2.0.1", 8083))
                ),
                Arguments.of("Ping message with many IPs (slow connection)",
                        true,
                        List.of(new InetSocketAddress("2001:db8::1", 8081),
                                new InetSocketAddress("127.0.0.1", 8082),
                                new InetSocketAddress("10.2.0.1", 8083))
                )
        );
    }

    @Test
    void shouldSerializeAndWriteHelloMessage() throws IOException {
        // Given
        FakeWritableByteChannel channel = newWritableChannel(false);
        InetSocketAddress expected = new InetSocketAddress("127.0.0.1", 8080);

        // When
        PacketSerializer.writeHelloMessage(channel, expected);

        // Then
        ByteBuffer dataWritten = channel.getDataWritten();
        ByteBuffer byteBuffer = PacketSerializer.readPacketFully(newReadableChannel(dataWritten));
        InetSocketAddress actual = IpPortSerializer.deserializeSingleIp(byteBuffer);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSerializeAndWriteHelloMessage_SlowConnection() throws IOException {
        // Given
        FakeWritableByteChannel channel = newWritableChannel(true);
        InetSocketAddress expected = new InetSocketAddress("127.0.0.1", 8080);

        // When
        PacketSerializer.writeHelloMessage(channel, expected);

        // Then
        ByteBuffer dataWritten = channel.getDataWritten();
        ByteBuffer byteBuffer = PacketSerializer.readPacketFully(newReadableChannel(dataWritten));
        InetSocketAddress actual = IpPortSerializer.deserializeSingleIp(byteBuffer);

        assertThat(actual).isEqualTo(expected);
    }

    @MethodSource("pingTestCases")
    @ParameterizedTest(name = "{0}")
    void shouldSerializeAndWritePingMessage_NoIps(String testName, boolean isSlowConnection, List<InetSocketAddress> ipList) throws IOException {
        // Given
        FakeWritableByteChannel channel = newWritableChannel(isSlowConnection);
        long timestamp = 42L;

        // When
        PacketSerializer.sendPing(channel, timestamp, ipList);

        // Then
        ByteBuffer dataWritten = channel.getDataWritten();

        ArrayList<InetSocketAddress> actualIps = new ArrayList<>();
        long actualTimetamp = PacketSerializer.receiveTimestamp(newReadableChannel(dataWritten), actualIps::addAll);

        assertThat(actualIps).containsExactlyElementsOf(ipList);
        assertThat(actualTimetamp).isEqualTo(timestamp);
    }
}
