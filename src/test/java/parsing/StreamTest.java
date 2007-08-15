package parsing;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import org.jvnet.mimepull.MIMEConfig;
import org.jvnet.mimepull.MIMEMessage;
import org.jvnet.mimepull.MIMEPart;

/**
 * @author Jitendra Kotamraju
 */
public class StreamTest extends TestCase {

    // Parts are accessed in order. The data is accessed using readOnce()
    // and there shouldn't be any data stored in temp files.
    public void testOrder() throws Exception {
        String boundary = "--boundary";
        int size = 123456789;
        MIMEConfig config = new MIMEConfig(false, 1024, 8192);
        MIMEMessage mm = new MIMEMessage(getInputStream(size), boundary , config);

        MIMEPart partA = mm.getPart("partA");
        verifyPart(partA.readOnce(), 0, size);

        MIMEPart partB = mm.getPart("partB");
        verifyPart(partB.readOnce(), 1, size);

        MIMEPart partC = mm.getPart("partC");
        verifyPart(partC.readOnce(), 2, size);
    }

    // partB, partA, partC are accessed in that order. Then partA should
    // go to disk. partB, and partC are accessed from in-memory
    public void testOutofOrder() throws Exception {
        String boundary = "--boundary";
        int size = 12345678;
        MIMEConfig config = new MIMEConfig(false, 1024, 8192);
        MIMEMessage mm = new MIMEMessage(getInputStream(size), boundary , config);

        MIMEPart partA = mm.getPart("partA");
        MIMEPart partB = mm.getPart("partB");
        MIMEPart partC = mm.getPart("partC");

        verifyPart(partB.readOnce(), 1, size);
        verifyPart(partA.readOnce(), 0, size);
        verifyPart(partC.readOnce(), 2, size);
    }

    private void verifyPart(InputStream in, int partNo, int size) throws Exception {
        int i = 0;
        int ch;
        while((ch=in.read()) != -1) {
            assertEquals((byte)('A'+(partNo+i++)%26), (byte)ch);
        }
        assertEquals(size, i);
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

}
