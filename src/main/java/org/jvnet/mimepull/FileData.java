package org.jvnet.mimepull;

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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Data createNext(MIMEPart part) {
        //return new FileData();
        return null;
    }
}
