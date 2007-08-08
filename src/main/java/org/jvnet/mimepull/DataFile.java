package org.jvnet.mimepull;

import java.io.RandomAccessFile;

/**
 * @author Kohsuke Kawaguchi
 */
class DataFile {
    private final RandomAccessFile file;
    private final long pointer;

    public void read( long pointer, byte[] buf, int start, int length ) {
        if(this.pointer!=pointer) {
            file.seek(pointer);
            this.pointer = pointer;
        }
        file.read(buf,start,length);
        pointer+=length;
    }
}
