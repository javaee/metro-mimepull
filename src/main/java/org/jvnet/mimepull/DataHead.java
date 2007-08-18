package org.jvnet.mimepull;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents an attachment part in a MIME message. MIME message parsing is done
 * lazily using a pull parser, so the part may not have all the data. {@link #read}
 * and {@link #readOnce} may trigger the actual parsing the message. In fact,
 * parsing of an attachment part may be triggered by calling {@link #read} methods
 * on some other attachemnt parts. All this happens behind the scenes so the
 * application developer need not worry about these details.
 *
 * @author Jitendra Kotamraju
 */
public class DataHead {

    /**
     * Linked list to keep the part's content
     */
    volatile Chunk head, tail;

    /**
     * If the part is stored in a file, non-null.
     *
     * If head is non-null, then we have the whole part in the file,
     * otherwise the file is only partial.
     */
    DataFile dataFile;

    private final MIMEPart part;
    private volatile int activeReads = 0;   // TODO sync + and -

    boolean readOnce;
    volatile long inMemory;                 // TODO sync + and -

    DataHead(MIMEPart part) {
        this.part = part;
    }

    void addBody(ByteBuffer buf) {
        synchronized(this) {
            inMemory += buf.limit();
        }

        if (tail!=null) {
            tail = tail.createNext(this, buf);
        } else {
            head = tail = new Chunk(new MemoryData(buf, part.msg.config));
        }
    }

    void doneParsing() {
        if (activeReads == 0 && dataFile != null) {
            head = tail = null;
            dataFile.close();
        }
    }


    /**
     * Can get the attachment part's content multiple times. That means
     * the full content needs to be there in memory or on the file system.
     * Calling this method would trigger parsing for the part's data. So
     * do not call this unless it is required(otherwise, just wrap MIMEPart
     * into a object that returns InputStream for e.g DataHandler)
     *
     * @return data for the part's content
     */
    public InputStream read() {
        if (readOnce) {
            throw new IllegalStateException("readOnce() is called before, read() cannot be called later.");
        }
        // Have the complete data on the file system
        if (part.parsed && dataFile != null && activeReads == 0) {
            return dataFile.getInputStream();
        }

        // Trigger parsing for the part
        while(tail == null) {
            if (!part.msg.makeProgress()) {
                throw new IllegalStateException("No such Part: "+part);
            }
        }

        if (head == null) {
            throw new IllegalStateException("Already read. Probably readOnce() is called before.");
        }
        synchronized(this) {
            ++activeReads;
        }
        return new ReadMultiStream();
    }


    /**
     * Can get the attachment part's content only once. The content
     * will be lost after the method. Content data is not be stored
     * on the file system or is not kept in the memory for the
     * following case:
     *   - Attachement parts contents are accessed sequentially
     *
     * In general, take advantage of this when the data is used only
     * once.
     *
     * @return data for the part's content
     */
    public InputStream readOnce() {
        if (readOnce) {
            throw new IllegalStateException("readOnce() is called before. It can only be called once.");
        }
        readOnce = true;
        if (part.parsed && dataFile != null && activeReads == 0) {
            return dataFile.getInputStream();
        }
        // Trigger parsing for the part
        while(tail == null) {
            if (!part.msg.makeProgress() && tail == null) {
                throw new IllegalStateException("No such Part: "+part);
            }
        }
        synchronized(this) {
            ++activeReads;
        }
        InputStream in = new ReadOnceStream();
        head = null;
        return in;
    }

    class ReadMultiStream extends InputStream {
        Chunk current;
        int offset;
        int len;
        byte[] buf;

        public ReadMultiStream() {
            this.current = head;
            len = current.data.size();
            buf = current.data.read();
        }

        @Override
        public int read(byte b[], int off, int sz) throws IOException {
            if(!fetch())    return -1;

            sz = Math.min(sz, len-offset);
            System.arraycopy(buf,offset,b,off,sz);
            return sz;
        }

        public int read() throws IOException {
            if (!fetch()) {
                synchronized(this) {
                    --activeReads;
                }
                return -1;
            }
            return (buf[offset++] & 0xff);
        }

        void adjustInMemoryUsage() {
            // Nothing to do in this case.
        }

        /**
         * Gets to the next chunk if we are done with the current one.
         * @return
         */
        private boolean fetch() {
            if (current == null) {
                throw new IllegalStateException("Stream already closed");
            }
            while(offset==len) {
                while(!part.parsed && current.next == null) {
                    part.msg.makeProgress();
                }
                current = current.next;

                if (current == null) {
                    return false;
                }
                adjustInMemoryUsage();
                this.offset = 0;
                this.buf = current.data.read();
                this.len = current.data.size();
            }
            return true;
        }

        public void close() throws IOException {
            super.close();
            current = null;
        }
    }

    final class ReadOnceStream extends ReadMultiStream {

        @Override
        void adjustInMemoryUsage() {
            synchronized(DataHead.this) {
                inMemory -= current.data.size();    // adjust current memory usage
            }
        }

    }


}
