package org.jvnet.mimepull;

import java.io.InputStream;

/**
 * @author Kohsuke Kawaguchi
 */
final class QueueInputStream extends InputStream {
    private final Queue q;

    QueueInputStream(Queue q) {
        this.q = q;
    }
}
