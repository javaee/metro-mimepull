package org.jvnet.mimepull;

import java.io.RandomAccessFile;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
class DataFile {
    private File file;
    private RandomAccessFile raf;
    private long pointer;


    public DataFile(File file) {
        /*
        this.file = file;
        raf = new RandomAccessFile(file,...);
        pointer=0;
        */
    }

    public void read( long pointer, byte[] buf, int start, int length ) {
        /*
        if(this.pointer!=pointer) {
            raf.seek(pointer);
            this.pointer = pointer;
        }
        raf.read(buf,start,length);
        pointer+=length;
        */
    }

    public void renameTo(File f) {
        /*
        raf.close();
        file.renameTo(f);
        raf = new RandomAccessFile(f);
        pointer = 0;
        */
    }
}
