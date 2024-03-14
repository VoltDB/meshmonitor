package org.voltdb.meshmonitor.testutils;

import java.nio.ByteBuffer;

public class FileChannelUtils {

    public static FakeWritableByteChannel newWritableChannel(boolean isSlowConnection) {
        return new FakeWritableByteChannel(isSlowConnection);
    }

    public static ByteBufferReadableByteChannel newReadableChannel(ByteBuffer data) {
        return new ByteBufferReadableByteChannel(data);
    }
}
