package org.voltdb.meshmonitor.serdes;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class IpPortSerializer {

    public static boolean serialize(ByteBuffer buffer, InetSocketAddress socketAddress) {
        byte[] address = socketAddress.getAddress().getAddress();
        int port = socketAddress.getPort();

        if (buffer.remaining() > Byte.BYTES + address.length + Short.BYTES) {
            buffer.put((byte) address.length);
            buffer.put(address);
            buffer.putShort((short) port);

            return true;
        }

        return false;
    }

    public static boolean serialize(ByteBuffer buffer, List<InetSocketAddress> socketAddresses) {
        for (InetSocketAddress socketAddress : socketAddresses) {
            if (!serialize(buffer, socketAddress)) {
                return false;
            }
        }

        return true;
    }

    public static List<InetSocketAddress> deserialize(ByteBuffer buffer) {
        List<InetSocketAddress> result = new ArrayList<>();

        while (buffer.hasRemaining()) {
            result.add(deserializeSingleIp(buffer));
        }

        return result;
    }

    public static InetSocketAddress deserializeSingleIp(ByteBuffer buffer) {
        byte addressLength = buffer.get();
        byte[] addressBytes = new byte[addressLength];
        buffer.get(addressBytes);
        int port = buffer.getShort() & 0xFFFF;

        InetAddress address;
        try {
            address = InetAddress.getByAddress(addressBytes);
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return new InetSocketAddress(address, port);
    }
}
