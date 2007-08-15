package org.jvnet.mimepull;

import java.io.*;

/**
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
class DataFile {
    private File file;
    private RandomAccessFile raf;
    private long writePointer;

    public DataFile(File file) {
        this.file = file;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        writePointer=0;
    }

    public InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch(FileNotFoundException fe) {
            throw new MIMEParsingException(fe);
        }
    }

    public void read( long pointer, byte[] buf, int start, int length ) {
        try {
            raf.seek(pointer);
            raf.read(buf,start,length);
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    public void renameTo(File f) {
        try {
            raf.close();
            file.renameTo(f);
            raf = new RandomAccessFile(f, "r");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    /**
     *
     *
     * @param data
     * @param offset
     * @param length
     * @return file pointer before the write operation
     */
    public long writeTo(byte[] data, int offset, int length) {
        try {
            long temp = writePointer;
            raf.seek(writePointer);
            raf.write(data, offset, length);
            writePointer = raf.getFilePointer();
            return temp;
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }
}
