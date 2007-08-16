package org.jvnet.mimepull;

import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;

/**
 * Keeps the Part's partial content data in memory.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
final class MemoryData implements Data {
    private final byte[] data;
    private final int len;
    private final MIMEConfig config;

    MemoryData(ByteBuffer buf, MIMEConfig config) {
        data = buf.array();
        len = buf.limit();
        this.config = config;
    }

    // size of the chunk given by the parser
    public int size() {
        return len;
    }

    public byte[] read() {
        return data;
    }

    public long writeTo(DataFile file) {
        return file.writeTo(data, 0, len);
    }

    /**
     * 
     * @param head
     * @param buf
     * @param part
     * @return
     */
    public Data createNext(Chunk head, ByteBuffer buf, MIMEPart part) {
        // TODO need to keep counter on part ??
        int counter = 0;
        while(head != null) {
            counter += head.data.size();
            head = head.next;
        }

        if (counter >= config.inMemorySize) {
            try {
                part.dataFile = new DataFile(File.createTempFile("MIME", "att"));
            } catch(IOException ioe) {
                throw new MIMEParsingException(ioe);
            }

            if (part.head != null) {
                long pointer = 0;
                // TODO: turn the whole thing to File from the byte 0
                for(Chunk c=part.head; c != null; c=c.next) {
                    c.data.writeTo(part.dataFile);
                    c.data = new FileData(part.dataFile, pointer, len);
                    pointer += len;
                }
            }
            return new FileData(part.dataFile, buf);
        } else {
            return new MemoryData(buf, config);
        }
    }
}
