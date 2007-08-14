package org.jvnet.mimepull;

import java.nio.ByteBuffer;

/**
 * @author Kohsuke Kawaguchi
 */
public class FileData implements Data {
    private final DataFile file;
    private final long offset;


    public FileData(DataFile file) {
        this.file = file;
        offset = 0;
    }

    public void readTo(byte[] buf, int start, int len) {
        
    }

    public void writeTo(DataFile file) {
        
    }

    public int size() {
        return 0;   // TODO
    }

    public Data createNext(ByteBuffer buf, MIMEPart part) {
        return new FileData(file);
    }
}
