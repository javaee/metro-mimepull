package org.jvnet.mimepull;

/**
 * @author Kohsuke Kawaguchi
 */
interface Data {
    // size should be fixed by Parser
    // int size();

    void readTo( byte[] buf, int start, int len );

    Data createNext(MIMEPart part);
}
