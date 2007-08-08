package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
public interface BufferedQueue extends Queue {
    BufferedQueue copy();
}
