package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
public class MemoryData implements Data {
    private final byte[] data;
    private final int len;

    public MemoryData(ByteArrayBuffer buf) {
        data = buf.getRawData();
        len = buf.size();
    }

    public Data toFile(DataFile f) {
        ...
    }
}
