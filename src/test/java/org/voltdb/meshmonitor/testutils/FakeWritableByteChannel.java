/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.testutils;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class FakeWritableByteChannel implements WritableByteChannel {

    private final boolean isSlow;
    private final ByteBuffer dataWritten = ByteBuffer.allocate(1024);

    public FakeWritableByteChannel(boolean isSlow) {
        this.isSlow = isSlow;
    }

    public ByteBuffer getDataWritten() {
        return dataWritten.duplicate().flip();
    }

    @Override
    public int write(ByteBuffer src) {
        int toBeWritten = src.remaining();
        if (isSlow) {
            toBeWritten = 1;
        }

        dataWritten.put(dataWritten.position(), src, src.position(), toBeWritten);
        dataWritten.position(dataWritten.position() + toBeWritten);
        src.position(src.position() + toBeWritten);

        return toBeWritten;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {
    }
}
