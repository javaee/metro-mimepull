package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
final class Chunk {
    Chunk next;
    Data data;

    public Chunk(Data data) {
        this.data = data;
    }

    public Chunk createNext(Chunk head, MIMEMessage msg) {
        //return next = new Chunk(data.createNext(0));
        return null;
    }
}
