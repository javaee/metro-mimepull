package org.jvnet.mimepull;

import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class MemoryData implements Data {
    private final byte[] data;
    private final int len;
    private int counter;
    private final MIMEConfig config;

    public MemoryData(ByteBuffer buf, MIMEConfig config) {
        data = buf.array();
        len = buf.limit();
        this.config = config;
    }

    // size of the chunk given by the parser
    public int size() {
        return len;
    }

    public void readTo(byte[] buf, int start, int len) {
        System.arraycopy(data, start, buf, start, len);
        counter -= len;
    }

    public void writeTo(DataFile file) {
    }

    public Data createNext(ByteBuffer buf, MIMEPart part) {
        counter += buf.limit();

        if (counter >= config.inMemorySize) {
            try {
                part.dataFile = new DataFile(File.createTempFile("MIME", "att"));
            } catch(IOException ioe) {
                throw new MIMEParsingException(ioe);
            }

            if (part.head != null) {
                // TODO: turn the whole thing to File from the byte 0
                for(Chunk c=part.head; c!=null; c=c.next) {
                    c.data.writeTo(part.dataFile);
                    c.data = new FileData(part.dataFile);
                }
            }

            return new FileData(part.dataFile);
        } else {
            return new MemoryData(buf, config);
        }
    }
}
