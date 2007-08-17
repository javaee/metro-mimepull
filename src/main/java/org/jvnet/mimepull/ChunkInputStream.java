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
        while(offset==len) {
            while(current.next==null || !part.parsed)
                msg.makeProgress();
            current = current.next;

            if(current==null)
                return -1;
            
            this.offset = 0;
            this.len = current.data.size();
        }

        return (buf[offset++] & 0xff);
    }

}
