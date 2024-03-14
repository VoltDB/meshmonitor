/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.serdes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IpPortSerializerTest {

    @Test
    void shouldSerializeAndDeserializeSingleIpPort() {
        // Givens
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        InetSocketAddress expected = new InetSocketAddress("127.0.0.1", 8080);

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, expected);
        buffer.flip();

        InetSocketAddress actual = IpPortSerializer.deserializeSingleIp(buffer);

        // Then
        assertThat(serializeResult).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSerializeAndDeserializeMultipleIpPorts() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        List<InetSocketAddress> expected = Arrays.asList(
                new InetSocketAddress("127.0.0.1", 8080),
                new InetSocketAddress("192.168.1.1", 80)
        );

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, expected);
        buffer.flip();

        List<InetSocketAddress> actual = IpPortSerializer.deserialize(buffer);

        // Then
        assertThat(serializeResult).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldHandleInsufficientBufferSpace() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(2);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, address);

        // Then
        assertThat(serializeResult).isFalse();
    }

    @Test
    void shouldHandleInsufficientBufferSpaceWhenSerializingManyIps() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(11);
        List<InetSocketAddress> addresses = Arrays.asList(
                new InetSocketAddress("127.0.0.1", 8080),
                new InetSocketAddress("192.168.1.1", 80)
        );

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, addresses);
        buffer.flip();
        List<InetSocketAddress> actual = IpPortSerializer.deserialize(buffer);

        // Then
        assertThat(serializeResult).isFalse();
        assertThat(actual).containsOnly(addresses.getFirst());
    }

    @Test
    void shouldSerializeAndDeserializeSingleIpv6() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        InetSocketAddress expected = new InetSocketAddress("2001:db8::1", 8080);

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, expected);
        buffer.flip();
        InetSocketAddress actual = IpPortSerializer.deserializeSingleIp(buffer);

        // Then
        assertThat(serializeResult).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSerializeAndDeserializeMultipleIpv6() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        List<InetSocketAddress> expected = Arrays.asList(
                new InetSocketAddress("2001:db8::1", 8080),
                new InetSocketAddress("fe80::1234", 80)
        );

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, expected);
        buffer.flip();
        List<InetSocketAddress> actual = IpPortSerializer.deserialize(buffer);

        // Then
        assertThat(serializeResult).isTrue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSerializeIpv6WithInsufficientBufferSpace() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(10);
        InetSocketAddress address = new InetSocketAddress("2001:db8::1", 8080);

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, address);

        // Then
        assertThat(serializeResult).isFalse();
    }

    @Test
    void shouldSerializeAndDeserializeMixOfIPv4AndIPv6Addresses() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        List<InetSocketAddress> expected = Arrays.asList(
                new InetSocketAddress("2001:db8::1", 8080),
                new InetSocketAddress("127.0.0.1", 8080)
        );

        // When
        boolean serializeResult = IpPortSerializer.serialize(buffer, expected);
        buffer.flip();
        List<InetSocketAddress> actual = IpPortSerializer.deserialize(buffer);

        // Then
        assertThat(serializeResult).isTrue();
        assertThat(actual).isEqualTo(expected);
    }
}
