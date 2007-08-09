package org.jvnet.mimepull;

import java.io.InputStream;

/**
 * @author Kohsuke Kawaguchi
 */
final class ChunkInputStream extends InputStream {
    Chunk current;
    int offset;

    public ChunkInputStream(Chunk startPos) {
        this.current = startPos;
    }
}
