package org.jvnet.mimepull;

import java.io.InputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
final class ChunkInputStream extends InputStream {
    Chunk current;
    int offset;

    public ChunkInputStream(Chunk startPos) {
        this.current = startPos;
    }

    public int read() throws IOException {
        return 0;
    }
}
