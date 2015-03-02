/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.jvnet.mimepull;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * @author Jitendra Kotamraju
 */
public class StreamTest extends TestCase {

    public void testOrderRead() throws Exception {
        testOrderRead(123456789);
    }

    public void testPartSize9195() throws Exception {
        testOrderRead(9195);
    }

    public void testPartSize9196() throws Exception {
        testOrderRead(9196);
    }

    public void testAllPartSizes() throws Exception {
        for (int size = 0; size < 50000; size++) {
            if (size %1000 == 0) {
                System.out.println("Trying for the size="+size);
            }
            try {
                testOrderRead(size);
            } catch (AssertionFailedError e) {
                System.out.println("Failed for part length " + size + " bytes");
                throw e;
            }
        }
    }

    public void testAllPartSizesForBufferedStream() throws Exception {
        for (int size = 0; size < 50000; size++) {
            if (size %1000 == 0) {
                System.out.println("Trying for the size="+size);
            }
            try {
                testOrderRead(size, getBufferedInputStream(size));
            } catch (AssertionFailedError e) {
                System.out.println("Failed for part length " + size + " bytes");
                throw e;
            }
        }
    }

    private void testOrderRead(int size, InputStream is) throws Exception {
        String boundary = "boundary";
        MIMEConfig config = new MIMEConfig();
        MIMEMessage mm = new MIMEMessage(is, boundary , config);

        MIMEPart partA = mm.getPart("partA");
        verifyPart(partA.read(), 0, size);
        partA.close();

        MIMEPart partB = mm.getPart("partB");
        verifyPart(partB.read(), 1, size);
        partB.close();

        MIMEPart partC = mm.getPart("partC");
        verifyPart(partC.read(), 2, size);
        partC.close();
    }

    private void testOrderRead(int size) throws Exception {
        testOrderRead(size, getInputStream(size));
    }

    // Parts are accessed in order. The data is accessed using readOnce()
    // and there shouldn't be any data stored in temp files.
    public void testOrderReadOnce() throws Exception {
        String boundary = "boundary";
        int size = 123456789;
        MIMEConfig config = new MIMEConfig();
        MIMEMessage mm = new MIMEMessage(getInputStream(size), boundary , config);

        MIMEPart partA = mm.getPart("partA");
        verifyPart(partA.readOnce(), 0, size);
        partA.close();

        MIMEPart partB = mm.getPart("partB");
        verifyPart(partB.readOnce(), 1, size);
        partB.close();

        MIMEPart partC = mm.getPart("partC");
        verifyPart(partC.readOnce(), 2, size);
        partC.close();
    }

    // partB, partA, partC are accessed in that order. Then partA should
    // go to disk. partB, and partC are accessed from in-memory
    public void testOutofOrderRead() throws Exception {
        String boundary = "boundary";
        int size = 12345678;
        MIMEConfig config = new MIMEConfig();
        MIMEMessage mm = new MIMEMessage(getInputStream(size), boundary , config);

        MIMEPart partA = mm.getPart("partA");
        MIMEPart partB = mm.getPart("partB");
        MIMEPart partC = mm.getPart("partC");

        verifyPart(partB.read(), 1, size);
        verifyPart(partA.read(), 0, size);
        verifyPart(partC.read(), 2, size);

        partA.close();
        partB.close();
        partC.close();
    }

    // partB, partA, partC are accessed in that order. Then partA should
    // go to disk. partB, and partC are accessed from in-memory
    public void testOutofOrderReadOnce() throws Exception {
        String boundary = "boundary";
        int size = 12345678;
        MIMEConfig config = new MIMEConfig();
        MIMEMessage mm = new MIMEMessage(getInputStream(size), boundary , config);

        MIMEPart partA = mm.getPart("partA");
        MIMEPart partB = mm.getPart("partB");
        MIMEPart partC = mm.getPart("partC");

        verifyPart(partB.readOnce(), 1, size);
        partB.close();
        verifyPart(partA.readOnce(), 0, size);
        partA.close();
        verifyPart(partC.readOnce(), 2, size);
        partC.close();
    }

    // MIMEPart.read() is called twice
    public void testOutofOrderMultipleRead() throws Exception {
        String boundary = "boundary";
        final int size = 12345678;
        MIMEConfig config = new MIMEConfig();
        MIMEMessage mm = new MIMEMessage(getInputStream(size), boundary , config);

        final MIMEPart partA = mm.getPart("partA");
        MIMEPart partB = mm.getPart("partB");
        MIMEPart partC = mm.getPart("partC");

        verifyPart(partB.readOnce(), 1, size);
        Thread[] threads = new Thread[2];

        for(int i=0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        verifyPart(partA.read(), 0, size);
                    } catch(Exception e) {
                        fail();
                    }
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        verifyPart(partC.readOnce(), 2, size);
        for (Thread thread : threads) {
            thread.join();
        }

        partA.close();
        partB.close();
        partC.close();
    }


    /*
    private void verifyPart(InputStream in, int partNo, int size) throws Exception {
        int i = 0;
        int ch;
        while((ch=in.read()) != -1) {
            assertEquals((byte)('A'+(partNo+i++)%26), (byte)ch);
        }
        assertEquals(size, i);
        in.close();
    }
    */

    private void verifyPart(InputStream in, int partNo, int size) throws Exception {
        byte[] buf = new byte[8192];
        int total = 0;
        int len;
        while((len=in.read(buf, 0, buf.length)) != -1) {
            for(int i=0; i < len; i++) {
                assertEquals((byte)('A'+(partNo+total+i)%26), buf[i]);
            }
            total += len;
        }
        assertEquals(size, total);
        in.close();
    }

    /*
     * partA's content ABC...ZAB...
     * partB's content BCD...ZAB...
     * partC's content CDE...ZAB...
     */
    private InputStream getInputStream(final int size) {
        final byte[] data = (
            "--boundary\r\n"+
            "Content-Type: text/plain\r\n"+
            "Content-Id: partA\r\n\r\n"+
            "1\r\n"+
            "--boundary\r\n"+
            "Content-Type: text/plain\r\n"+
            "Content-ID: partB\r\n\r\n"+
            "2\r\n"+
            "--boundary\r\n"+
            "Content-Type: text/plain\r\n"+
            "Content-ID: partC\r\n\r\n"+
            "3\r\n"+
            "--boundary--").getBytes();

        return new InputStream() {
            int i, j;

            @Override
            public int read() throws IOException {
                if (i >= data.length) {
                    return -1;
                } else if (data[i] == '1' || data[i] == '2' || data[i] == '3') {
                    if (j < size) {
                        int partNo = data[i]-'1';
                        return (byte)('A'+(partNo+j++)%26);
                    } else {
                        j = 0; i++;
                    }
                }
                return data[i++];
            }
        };

    }

    private InputStream getBufferedInputStream(int size) {
        return new BufferedInputStream(getInputStream(size));
    }

}
