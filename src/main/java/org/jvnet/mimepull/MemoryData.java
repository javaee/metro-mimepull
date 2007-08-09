package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
public class MemoryData implements Data {
    private final byte[] data;
    private final int len;
    private int counter;

    public MemoryData(ByteArrayBuffer buf) {
        data = buf.getRawData();
        len = buf.size();
    }

    public Data createNext(MIMEPart part) {
        if(counter==msg.threshold) {
            part.dataFile = new DataFile();

            if(part.head!=null) {
                // TODO: turn the whole thing to File from the byte 0
                for(Chunk c=part.head; c!=null; c=c.next) {
                    c.data.writeTo(part.dataFile);
                    c.data = new FileData(...);
                }
            }

            return new FileData(part.dataFile);
        } else
            return new MemoryData();
    }
}
