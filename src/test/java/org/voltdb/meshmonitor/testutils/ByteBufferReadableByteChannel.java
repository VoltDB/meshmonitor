/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.testutils;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import static org.voltdb.meshmonitor.testutils.FileChannelUtils.byteBufferAbsolutePut;

public class ByteBufferReadableByteChannel implements ReadableByteChannel {

    private final ByteBuffer data;

    ByteBufferReadableByteChannel(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public int read(ByteBuffer dst) {
        int remaining = dst.remaining();

        byteBufferAbsolutePut(dst, dst.position(), data, data.position(), remaining);

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
