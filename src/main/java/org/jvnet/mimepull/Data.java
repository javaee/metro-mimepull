package org.jvnet.mimepull;

import java.nio.ByteBuffer;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
interface Data {

    // size of the chunk given by the parser
    int size();

    void readTo( byte[] buf, int start, int len );

    /**
     *
     * @param file
     * @return file pointer before the write operation
     */
    long writeTo(DataFile file);

    Data createNext(Chunk head, ByteBuffer buf, MIMEPart msg);
}
