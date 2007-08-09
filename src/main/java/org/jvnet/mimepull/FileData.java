package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
public class FileData implements Data {
    private final DataFile file;
    private final long offset;


    public FileData(DataFile file) {
        this.file = file;
    }

    public Data createNext(MIMEPart part) {
        return new FileData();
    }
}
