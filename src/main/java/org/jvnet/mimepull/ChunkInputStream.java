package org.jvnet.mimepull;

import java.io.InputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
final class ChunkInputStream extends InputStream {
    Chunk current;
    int offset;
    int len;

    public ChunkInputStream(Chunk startPos) {
        this.current = startPos;
        len = current.data.size();
    }

    public int read() throws IOException {
        /*
        if (offset < len) {
            byte[] bytes = new byte[1];
            current.data.readTo(bytes, offset, 1);
            return bytes[0];
        }
        current = current.next;
        while (current == null) {
            if (!msg.makeProgress()) {
            }
            current = current.next;
        }
        this.offset = 0;
        this.len = current.data.size();
        if (offset < len) {
            byte[] bytes = new byte[1];
            current.data.readTo(bytes, offset, 1);
            return bytes[0];
        }
        */
        return -1;
    }

}
