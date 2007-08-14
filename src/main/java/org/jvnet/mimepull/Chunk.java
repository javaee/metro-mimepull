package org.jvnet.mimepull;

import java.nio.ByteBuffer;

/**
 * @author Kohsuke Kawaguchi
 */
final class Chunk {
    Chunk next;
    Data data;

    public Chunk(Data data) {
        this.data = data;
    }

    public Chunk createNext(Chunk head, MIMEPart part, ByteBuffer buf) {
        return next = new Chunk(data.createNext(buf, part));
    }
}
