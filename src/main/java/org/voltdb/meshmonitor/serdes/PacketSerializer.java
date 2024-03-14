/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.serdes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.function.Consumer;

public class PacketSerializer {

    public static ByteBuffer readPacketFully(ReadableByteChannel socketChannel) throws IOException {
        ByteBuffer buffer = readCompletely(4, socketChannel);

        int packetSize = buffer.getInt();
        return readCompletely(packetSize, socketChannel);
    }

    public static void writeHelloMessage(WritableByteChannel channel, InetSocketAddress self) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.putInt(0);

        IpPortSerializer.serialize(byteBuffer, self);
        byteBuffer.putInt(0, byteBuffer.position() - 4);
        byteBuffer.flip();

        writeCompletely(channel, byteBuffer);
    }

    public static void sendPing(WritableByteChannel channel, long now, List<InetSocketAddress> servers) throws IOException {
        ByteBuffer byteBuf = ByteBuffer.allocate(1024);

        byteBuf.putInt(0);
        byteBuf.putLong(now);

        IpPortSerializer.serialize(byteBuf, servers);
        byteBuf.putInt(0, byteBuf.position() - 4);
        byteBuf.flip();

        writeCompletely(channel, byteBuf);
    }

    public static long receiveTimestamp(ReadableByteChannel channel, Consumer<List<InetSocketAddress>> meshConsumer) throws IOException {
        ByteBuffer buffer = readCompletely(4, channel);

        int packetSize = buffer.getInt();
        buffer = readCompletely(packetSize, channel);

        long timestamp = buffer.getLong();
        meshConsumer.accept(IpPortSerializer.deserialize(buffer));

        return timestamp;
    }

    private static ByteBuffer readCompletely(int packetSize, ReadableByteChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(packetSize);
        while (buffer.hasRemaining()) {
            socketChannel.read(buffer);
        }

        buffer.flip();
        return buffer;
    }

    private static void writeCompletely(WritableByteChannel channel, ByteBuffer byteBuffer) throws IOException {
        while (byteBuffer.hasRemaining()) {
            channel.write(byteBuffer);
        }
    }
}
