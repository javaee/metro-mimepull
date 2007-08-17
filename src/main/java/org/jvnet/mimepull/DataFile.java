package org.jvnet.mimepull;

import java.io.*;

/**
 * Use {@link RandomAccessFile} for concurrent access of read
 * and write partial part's content.
 *
 * TODO Should we worry about RandomAccessFile.close(). But if
 * TODO we close(), then we cannot do read or write().
 *
 * TODO when do we delete this file ??
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
final class DataFile {
    private File file;
    private RandomAccessFile raf;
    private long writePointer;

    DataFile(File file) {
        this.file = file;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        writePointer=0;
    }

    /**
     * TODO When should we call this ? When all the data is in the file,
     * there is no point in calling read(), write(). Directly can use
     * {#getInputStream()}
     */
    void close() {
        try {
            raf.close();
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    /**
     * TODO should we raf.close() before returning the stream
     *
     * @return
     */
    InputStream getInputStream() {
        try {
            return new DirectInputStream(this);
        } catch(FileNotFoundException fe) {
            throw new MIMEParsingException(fe);
        }
    }

    /**
     * Read data from the given file pointer position.
     *
     * @param pointer read position
     * @param buf that needs to be filled
     * @param offset the start offset of the data.
     * @param length of data that needs to be read
     */
    synchronized void read(long pointer, byte[] buf, int offset, int length ) {
        try {
            raf.seek(pointer);
            raf.readFully(buf, offset, length);
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    void renameTo(File f) {
        try {
            raf.close();
            file.renameTo(f);
            raf = new RandomAccessFile(f, "r");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    /**
     * Write data to the file
     *
     * @param data that needs to written to a file
     * @param offset start offset in the data
     * @param length no bytes to write
     * @return file pointer before the write operation(or at which the
     *         data is written)
     */
    synchronized long writeTo(byte[] data, int offset, int length) {
        try {
            long temp = writePointer;
            raf.seek(writePointer);
            raf.write(data, offset, length);
            writePointer = raf.getFilePointer();    // Update pointer for next write
            return temp;
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    /**
     * Keeps {@link DataFile} from garbage collected when
     * someone is reading it.
     */
    private static final class DirectInputStream extends FilterInputStream {
        private DataFile parent;
        public DirectInputStream(DataFile parent) throws FileNotFoundException {
            super(new FileInputStream(parent.file));
            this.parent = parent;
        }

        public void close() throws IOException {
            super.close();
            parent = null;
        }
    }
}
