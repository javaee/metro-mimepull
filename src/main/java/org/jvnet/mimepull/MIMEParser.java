/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Pull parser for the MIME messages. Applications can use pull API to continue
 * the parsing MIME messages lazily.
 *
 * <pre>
 * for e.g.:
 * <p>
 *
 * MIMEParser parser = ...
 * Iterator<MIMEEvent> it = parser.iterator();
 * while(it.hasNext()) {
 *   MIMEEvent event = it.next();
 *   ...
 * }
 * </pre>
 *
 * Original parsing code is taken from java mail's MIME parser.
 *
 * @author Jitendra Kotamraju
 */
class MIMEParser implements Iterable<MIMEEvent> {
    private enum STATE {START_MESSAGE, SKIP_PREAMBLE, START_PART, HEADERS, BODY, END_PART, END_MESSAGE}
    private STATE state = STATE.START_MESSAGE;

    private final InputStream in;
    private final String boundary;
    private final byte[] bndbytes;
    private final int bl;
    private final MIMEConfig config;
    /**
     * Have we parsed the data from our InputStream yet?
     */
    private boolean parsed;

    /*
     * Read and process body partsList until we see the
     * terminating boundary line (or EOF).
     */
    private boolean done = false;


    private boolean bol = true;    // beginning of line flag
    // the two possible end of line characters
    private int eol1 = -1, eol2 = -1;

    private int currentSize;

    MIMEParser(InputStream in, String boundary, MIMEConfig config) {
        this.in = (in instanceof ByteArrayInputStream || in instanceof BufferedInputStream)
                ? in :  new BufferedInputStream(in);
        this.boundary = boundary;
        this.bndbytes = getBytes(boundary);
        bl = bndbytes.length;
        this.config = config;
    }

    /**
     * Returns iterator for the parsing events. Use the iterator to advance
     * the parsing.
     *
     * @return iterator for parsing events
     */
    public Iterator<MIMEEvent> iterator() {
        return new MIMEEventIterator();
    }

    class MIMEEventIterator implements Iterator<MIMEEvent> {

        public boolean hasNext() {
            return !parsed;
        }

        public MIMEEvent next() {
            switch(state) {
                case START_MESSAGE :
                    state = STATE.SKIP_PREAMBLE;
                    return MIMEEvent.START_MESSAGE;

                case SKIP_PREAMBLE :
                    skipPreamble();
                    // fall through
                case START_PART :
                    state = STATE.HEADERS;
                    return MIMEEvent.START_PART;

                case HEADERS :
                    InternetHeaders ih = readHeaders();
                    state = STATE.BODY;
                    currentSize = config.firstChunkSize;
                    return new MIMEEvent.Headers(ih);

                case BODY :
                    ByteArrayBuffer buf = readBody();
                    currentSize = config.nextChunkSize;
                    return new MIMEEvent.Content(buf);

                case END_PART :
                    if (done) {
                        state = STATE.END_MESSAGE;
                    } else {
                        state = STATE.START_PART;
                    }
                    return MIMEEvent.END_PART;

                case END_MESSAGE :
                    parsed = true;
                    return MIMEEvent.END_MESSAGE;

                default :
                    throw new MIMEParsingException("Unknown Parser state = "+state);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Collects the headers for the current part by parsing mesage stream.
     *
     * @return headers for the current part
     */
    private InternetHeaders readHeaders() {
        return new InternetHeaders(in);
    }

    /**
     * Reads and saves the part of the current attachment part's content.
     *
     * @return a chunk of the part's content
     */
    private ByteArrayBuffer readBody() {
        int b;
        try {
            if (!in.markSupported())
		        throw new MIMEParsingException("Stream doesn't support mark");
            ByteArrayBuffer buf = new ByteArrayBuffer(currentSize);
            for (; ;) {
                if (bol) {
                    /*
                    * At the beginning of a line, check whether the
                    * next line is a boundary.
                    */
                    int i;
                    in.mark(bl + 4 + 1000); // bnd + "--\r\n" + lots of LWSP
                    // read bytes, matching against the boundary
                    for (i = 0; i < bl; i++)
                        if (in.read() != bndbytes[i])
                            break;
                    if (i == bl) {
                        // matched the boundary, check for last boundary
                        int b2 = in.read();
                        if (b2 == '-') {
                            if (in.read() == '-') {
                                done = true;
                                state = STATE.END_PART;
                                break;    // ignore trailing text
                            }
                        }
                        // skip linear whitespace
                        while (b2 == ' ' || b2 == '\t')
                            b2 = in.read();
                        // check for end of line
                        if (b2 == '\n') {
                            state = STATE.END_PART;
                            break;    // got it!  break out of the loop
                        }
                        if (b2 == '\r') {
                            in.mark(1);
                            if (in.read() != '\n')
                                in.reset();
                            state = STATE.END_PART;
                            break;    // got it!  break out of the loop
                        }
                    }
                    // failed to match, reset and proceed normally
                    in.reset();

                    // if this is not the first line, write out the
                    // end of line characters from the previous line
                    if (buf != null && eol1 != -1) {
                        buf.write(eol1);
                        if (eol2 != -1)
                            buf.write(eol2);
                        eol1 = eol2 = -1;
                    }
                }

                // read the next byte
                if ((b = in.read()) < 0) {
                    state = STATE.END_PART;
                    done = true;
                    break;
                }

                /*
                * If we're at the end of the line, save the eol characters
                * to be written out before the beginning of the next line.
                */
                if (b == '\r' || b == '\n') {
                    bol = true;
                    eol1 = b;
                    if (b == '\r') {
                        in.mark(1);
                        if ((b = in.read()) == '\n')
                            eol2 = b;
                        else
                            in.reset();
                    }
                } else {
                    bol = false;
                    if (buf != null)
                        buf.write(b);
                }
                // Reached our content chunk size. Still in the same part.
                if (buf.size() >= currentSize) {
                    state = STATE.BODY;
                    break;
                }
            }
            return buf;
        } catch (IOException ioex) {
            throw new MIMEParsingException("IO Error", ioex);
        }
    }

    /**
     * Skips the preamble to find the first attachment part
     */
    private void skipPreamble() {
        try {
            // Skip the preamble
            LineInputStream lin = new LineInputStream(in);
            String line;
            while ((line = lin.readLine()) != null) {
            /*
             * Strip trailing whitespace.  Can't use trim method
             * because it's too aggressive.  Some bogus MIME
             * messages will include control characters in the
             * boundary string.
             */
            int i;
            for (i = line.length() - 1; i >= 0; i--) {
                char c = line.charAt(i);
                if (!(c == ' ' || c == '\t'))
                break;
            }
            line = line.substring(0, i + 1);
            if (line.equals(boundary))
                break;
            }
            if (line == null)
                throw new MIMEParsingException("Missing start boundary");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    private static byte[] getBytes(String s) {
        char [] chars= s.toCharArray();
        int size = chars.length;
        byte[] bytes = new byte[size];

        for (int i = 0; i < size;)
            bytes[i] = (byte) chars[i++];
        return bytes;
    }
    
}
