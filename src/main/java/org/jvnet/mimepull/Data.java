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

    void writeTo(DataFile file);

    Data createNext(ByteBuffer buf, MIMEPart msg);
}
