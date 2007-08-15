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

    /**
     * Creates a new chunk and adds to linked list.
     *
     * @param head of the linked list
     * @param part MIME part for which data is got
     * @param buf MIME part partial data
     * @return created chunk
     */
    public Chunk createNext(Chunk head, MIMEPart part, ByteBuffer buf) {
        return next = new Chunk(data.createNext(head, buf, part));
    }
}
