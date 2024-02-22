package org.voltdb.meshmonitor.serdes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.function.Consumer;

public class PacketSerializer {

    public static ByteBuffer readPacketFully(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        while (buffer.hasRemaining()) {
            socketChannel.read(buffer);
        }
        buffer.flip();

        buffer = ByteBuffer.allocate(buffer.getInt());
        while (buffer.hasRemaining()) {
            socketChannel.read(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static void writeHelloMessage(SocketChannel channel, InetSocketAddress self) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.putInt(0);

        IpPortSerializer.serialize(byteBuffer, self);
        byteBuffer.putInt(0, byteBuffer.position() - 4);
        byteBuffer.flip();

        channel.write(byteBuffer);
    }

    public static void sendPing(SocketChannel channel, long now, List<InetSocketAddress> servers) throws IOException {
        ByteBuffer byteBuf = ByteBuffer.allocate(1024);

        byteBuf.putInt(0);
        byteBuf.putLong(now);

        IpPortSerializer.serialize(byteBuf, servers);
        byteBuf.putInt(0, byteBuf.position() - 4);
        byteBuf.flip();

        while (byteBuf.hasRemaining()) {
            channel.write(byteBuf);
        }
    }

    public static long receiveTimestamp(SocketChannel channel, Consumer<List<InetSocketAddress>> meshConsumer) throws IOException {
        ByteBuffer recvBuf = ByteBuffer.allocate(4);
        while (recvBuf.hasRemaining()) {
            channel.read(recvBuf);
        }
        recvBuf.flip();

        int packetSize = recvBuf.getInt();
        recvBuf = ByteBuffer.allocate(packetSize);
        while (recvBuf.hasRemaining()) {
            channel.read(recvBuf);
        }
        recvBuf.flip();

        long timestamp = recvBuf.getLong();
        meshConsumer.accept(IpPortSerializer.deserialize(recvBuf));
        return timestamp;
    }
}
