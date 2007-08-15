package org.jvnet.mimepull;

import java.nio.ByteBuffer;

/**
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class FileData implements Data {
    private final DataFile file;
    private final long pointer;
    private final int length;

    public FileData(DataFile file, ByteBuffer buf) {
        this(file, file.writeTo(buf.array(), 0, buf.limit()), buf.limit());
    }

    public FileData(DataFile file, long pointer, int length) {
        this.file = file;
        this.pointer = pointer;
        this.length = length;
    }

    public byte[] read() {
        byte[] buf = new byte[length];
        file.read(pointer, buf, 0, length);
        return buf;
    }

    public long writeTo(DataFile file) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return length;
    }

    public Data createNext(Chunk head, ByteBuffer buf, MIMEPart part) {
        return new FileData(file, buf);
    }
}
