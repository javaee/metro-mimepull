package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
interface Queue {
    /**
     * Writes data to the queue and transfers the ownership of the byte[].
     */
    void push(byte[] buf, int size);

    /**
     * Marks EOF. All bytes written.
     */
    void close();

    /**
     * Read bytes from the queue.
     *
     * @return
     *      -1 for EOF. Otherwise # of bytes read.
     */
    int pop(byte[] buf, int offset, int size);
}
