package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
public class FileData implements Data {
    private final DataFile file;
    private final long offset;

    public Data toFile(DataFile f) {
        return this;
    }
}
