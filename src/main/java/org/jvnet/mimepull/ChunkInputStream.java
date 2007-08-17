package org.jvnet.mimepull;

import java.io.InputStream;
import java.io.IOException;

/**
 * Constructs a InputStream from a linked list of {@link Chunk}s.
 * 
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
final class ChunkInputStream extends InputStream {
    Chunk current;
    int offset;
    int len;
    final MIMEMessage msg;
    final MIMEPart part;
    byte[] buf;

    public ChunkInputStream(MIMEMessage msg, MIMEPart part, Chunk startPos) {
        this.current = startPos;
        len = current.data.size();
        buf = current.data.read();
        this.msg = msg;
        this.part = part;
    }

    public int read() throws IOException {
        if (offset < len) {
            return (buf[offset++] & 0xff);
        }
        while(true) {
            if (current.next == null && part.parsed) {     // TODO sync or volatile
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
        buf = current.data.read();
        if (offset < len) {
            return buf[offset++];
        }
        return -1;
    }

}
