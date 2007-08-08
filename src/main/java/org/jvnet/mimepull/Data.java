package org.jvnet.mimepull;

import java.nio.ByteBuffer;

/**
 * @author Kohsuke Kawaguchi
 */
interface Data {
    // size should be fixed by Parser
    // int size();

    void readTo( byte[] buf, int start, int len );

    Data toFile(DataFile f);
}
