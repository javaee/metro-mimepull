package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
final class Chunk {
    final Chunk next;
    Data data;

    public Chunk(Chunk next, Data data) {
        this.next = next;
        this.data = data;
    }
}
