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


    public FileData(DataFile file, long pointer, int length) {
        this.file = file;
        this.pointer = pointer;
        this.length = length;
    }

    public void readTo(byte[] buf, int start, int len) {
        file.read(pointer,buf,start,len);
    }

    public long writeTo(DataFile file) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return length;
    }

    public Data createNext(Chunk head, ByteBuffer buf, MIMEPart part) {
        long pointer = file.writeTo(buf.array(), 0, buf.limit());
        return new FileData(file, pointer, buf.limit());
    }
}
