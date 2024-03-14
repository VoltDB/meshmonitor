package org.voltdb.meshmonitor.testutils;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ByteBufferReadableByteChannel implements ReadableByteChannel {

    private final ByteBuffer data;

    ByteBufferReadableByteChannel(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public int read(ByteBuffer dst) {
        int remaining = dst.remaining();
        dst.put(dst.position(), data, data.position(), remaining);

        dst.position(dst.position() + remaining);
        data.position(data.position() + remaining);

        return remaining;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {

    }
}
