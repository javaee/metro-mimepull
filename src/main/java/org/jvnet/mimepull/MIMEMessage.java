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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Represents MIME message. MIME message parsing is done lazily using a
 * pull parser.
 *
 * @author Jitendra Kotamraju
 */
public class MIMEMessage {
    MIMEConfig config;

    private final InputStream in;
    private final List<MIMEPart> partsList;
    private final Map<String, MIMEPart> partsMap;
    private final Iterator<MIMEEvent> it;
    private boolean parsed;     // true when entire message is parsed
    private MIMEPart currentPart;
    private int currentIndex;

    /**
     * @see MIMEMessage(InputStream, String)
     */
    public MIMEMessage(InputStream in, String boundary) {
        this(in, boundary, new MIMEConfig());
    }

    /**
     * Creates a MIME message from the content's stream.
     *
     * @param in MIME message stream
     * @param boundary the separator for parts(pass it without --)
     * @param config various configuration parameters
     */
    public MIMEMessage(InputStream in, String boundary, MIMEConfig config) {
        this.in = in;
        this.config = config;
        MIMEParser parser = new MIMEParser(in, boundary, config);
        it = parser.iterator();

        partsList = new ArrayList<MIMEPart>();
        partsMap = new HashMap<String, MIMEPart>();
        if (config.isParseEagerly()) {
            parseAll();
        }
    }

    /**
     * Gets all the attachments by parsing the entire MIME message. Avoid
     * this if possible since it is an expensive operation.
     *
     * @return list of attachments.
     */
    public List<MIMEPart> getAttachments() {
        if (!parsed) {
            parseAll();
        }
        return partsList;
    }

    /**
     * Creates nth attachment lazily. It doesn't validate
     * if the message has so many attachments. To
     * do the validation, the message needs to be parsed.
     * The parsing of the message is done lazily and is done
     * while reading the bytes of the part.
     *
     * @param index sequential order of the part. starts with zero.
     * @return attachemnt part
     */
    public MIMEPart getPart(int index) {
        MIMEPart part = (index < partsList.size()) ? partsList.get(index) : null;
        if (parsed && part == null) {
            throw new MIMEParsingException("There is no "+index+" attachment part ");
        }
        if (part == null) {
            // Parsing will done lazily and will be driven by reading the part
            part = new MIMEPart(this);
            partsList.add(index, part);
        }
        return part;
    }

    /**
     * Creates a lazy attachment for a given Content-ID. It doesn't validate
     * if the message contains an attachment with the given Content-ID. To
     * do the validation, the message needs to be parsed. The parsing of the
     * message is done lazily and is done while reading the bytes of the part.
     *
     * @param contentId Content-ID of the part, expects Content-ID without <, >
     * @return attachemnt part
     */
    public MIMEPart getPart(String contentId) {
        MIMEPart part = getDecodedCidPart(contentId);
        if (parsed && part == null) {
            throw new MIMEParsingException("There is no attachment part with Content-ID = "+contentId);
        }
        if (part == null) {
            // Parsing is done lazily and is driven by reading the part
            part = new MIMEPart(this, contentId);
            partsMap.put(contentId, part);
        }
        return part;
    }

    // this is required for Indigo interop, it writes content-id without escaping
    private MIMEPart getDecodedCidPart(String cid) {
        MIMEPart part = partsMap.get(cid);
        if (part == null) {
            if (cid.indexOf('%') != -1) {
                try {
                    String tempCid = URLDecoder.decode(cid, "utf-8");
                    part = partsMap.get(tempCid);
                } catch(UnsupportedEncodingException ue) {
                    // Ignore it
                }
            }
        }
        return part;
    }


    /**
     * Parses the whole MIME message eagerly
     */
    public void parseAll() {
        while(makeProgress()) {
            // Nothing to do
        }
    }



    /**
     * Parses the MIME message in a pull fashion.
     *
     * @return
     *      false if the parsing is completed.
     */
    public synchronized boolean makeProgress() {
        if (!it.hasNext()) {
            return false;
        }

        MIMEEvent event = it.next();

        switch(event.getEventType()) {
            case START_MESSAGE :
                break;

            case START_PART :
                break;

            case HEADERS :
                MIMEEvent.Headers headers = (MIMEEvent.Headers)event;
                InternetHeaders ih = headers.getHeaders();
                List<String> cids = ih.getHeader("content-id");
                String cid = (cids != null) ? cids.get(0) : currentIndex+"";
                if (cid.length() > 2 && cid.charAt(0)=='<') {
                    cid = cid.substring(1,cid.length()-1);
                }
                MIMEPart listPart = (currentIndex < partsList.size()) ? partsList.get(currentIndex) : null;
                MIMEPart mapPart = getDecodedCidPart(cid);
                if (listPart == null && mapPart == null) {
                    currentPart = getPart(cid);
                    partsList.add(currentIndex, currentPart);
                } else if (listPart == null) {
                    currentPart = mapPart;
                    partsList.add(currentIndex, mapPart);
                } else if (mapPart == null) {
                    currentPart = listPart;
                    currentPart.setContentId(cid);
                    partsMap.put(cid, currentPart);
                } else if (listPart != mapPart) {
                    throw new MIMEParsingException("Created two different attachments using Content-ID and index");
                }
                currentPart.setHeaders(ih);
                break;

            case CONTENT :
                MIMEEvent.Content content = (MIMEEvent.Content)event;
                ByteBuffer buf = content.getData();
                currentPart.addBody(buf);
                break;

            case END_PART :
                currentPart.doneParsing();
                ++currentIndex;
                break;

            case END_MESSAGE :
                parsed = true;
                try {
                    in.close();
                } catch(IOException ioe) {
                    throw new MIMEParsingException(ioe);
                }
                break;

            default :
                throw new MIMEParsingException("Unknown Parser state = "+event.getEventType());
        }
        return true;
    }
}
