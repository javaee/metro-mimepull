package org.jvnet.mimepull;

import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
class DataFile {
    private File file;
    private RandomAccessFile raf;
    private long pointer;


    public DataFile(File file) {
        this.file = file;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        pointer=0;
    }

    public void read( long pointer, byte[] buf, int start, int length ) {
        if (this.pointer != pointer) {
            try {
                raf.seek(pointer);
            } catch(IOException ioe) {
                throw new MIMEParsingException(ioe);
            }
            this.pointer = pointer;
        }
        try {
            raf.read(buf,start,length);
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        pointer+=length;
    }

    public void renameTo(File f) {
        try {
            raf.close();
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        file.renameTo(f);
        try {
            raf = new RandomAccessFile(f, "rw");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        pointer = 0;
    }
}
