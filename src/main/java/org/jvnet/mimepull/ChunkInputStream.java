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
    final MIMEMessage msg;
    final MIMEPart part;
    byte[] buf;         // TODO no reallocation; use one size

    public ChunkInputStream(MIMEMessage msg, MIMEPart part, Chunk startPos) {
        this.current = startPos;
        len = current.data.size();
        buf = new byte[len];                // TODO reuse
        current.data.readTo(buf, 0, len);
        this.msg = msg;
        this.part = part;
    }

    public int read() throws IOException {
        if (offset < len) {
            return buf[offset++];
        }
        while(true) {
            if (current.next == null && part.parsed) {     // TOD sync or volatile
                return -1;
            }
            if (current.next == null)
                msg.makeProgress();
            else
                break;
        }
        current = current.next;
        assert current != null;
        this.offset = 0;
        this.len = current.data.size();
        buf = new byte[len];            // TODO reuse
        current.data.readTo(buf, 0, len);
        if (offset < len) {
            return buf[offset++];
        }
        return -1;
    }

}
