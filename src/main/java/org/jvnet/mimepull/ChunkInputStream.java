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

    public ChunkInputStream(MIMEMessage msg, MIMEPart part, Chunk startPos) {
        this.current = startPos;
        len = current.data.size();
        this.msg = msg;
        this.part = part;
    }

    public int read() throws IOException {
        if (offset < len) {
            byte[] bytes = new byte[1];
            current.data.readTo(bytes, offset++, 1);
            return bytes[0];
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
        if (offset < len) {
            byte[] bytes = new byte[1];
            current.data.readTo(bytes, offset++, 1);
            return bytes[0];
        }
        return -1;
    }

}
