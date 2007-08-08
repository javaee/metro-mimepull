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
import java.util.*;

/**
 * Represents MIME message. MIME message parsing is done lazily using a
 * pull parser.
 *
 * @author Jitendra Kotamraju
 */
public class MIMEMessage {
    private final MIMEParser parser;
    private final MIMEConfig config;

    private final List<MIMEPart> partsList;
    private final Map<String, MIMEPart> partsMap;
    private final Iterator<MIMEEvent> it;
    private boolean parsed = false;


    public MIMEMessage(InputStream in, String boundary, MIMEConfig config) {
        this.config = config;
        parser = new MIMEParser(in, boundary, config);
        it = parser.iterator();

        partsList = new ArrayList<MIMEPart>();
        partsMap = new HashMap<String, MIMEPart>();
    }

    /**
     * TODO
     * @return
     */
    public List<MIMEPart> getAttachments() {
        if (!parsed) {
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
        MIMEPart part = partsList.get(index);
        if (parsed && part == null) {
            throw new MIMEParsingException("There is no "+index+" attachment part ");
        }
        if (part == null) {
            // Parsing will done lazily and will be driven by reading the part
            part = new MIMEPart(config);
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
     * @param contentId Content-ID of the part
     * @return attachemnt part
     */
    public MIMEPart getPart(String contentId) {
        MIMEPart part = partsMap.get(contentId);
        if (parsed && part == null) {
            throw new MIMEParsingException("There is no attachment part with Content-ID = "+contentId);
        }
        if (part == null) {
            // Parsing is done lazily and is driven by reading the part
            part = new MIMEPart(config, contentId);
            partsMap.put(contentId, part);
        }
        return part;
    }


    /**
     *
     */
    public void parseAll() {
        MIMEPart currentPart = null;
        int currentIndex = 0;

        while(it.hasNext()) {
            MIMEEvent event = it.next();

            switch(event.getEventType()) {
                case START_MESSAGE :
                    break;

                case START_PART :
                    break;

                case HEADERS :
                    MIMEEvent.Headers headers = (MIMEEvent.Headers)event;
                    InternetHeaders ih = headers.getHeaders();
                    String [] cids = ih.getHeader("content-id");
                    String cid = (cids != null) ? cids[0] : partsList.size()+"";
                    if (cid.length() > 2 && cid.charAt(0)=='<') {
                        cid = cid.substring(1,cid.length()-1);
                    }
                    MIMEPart listPart = (currentIndex < partsList.size()) ? partsList.get(currentIndex) : null;
                    MIMEPart mapPart = partsMap.get(cid);
                    if (listPart == null && mapPart == null) {
                        currentPart = getPart(cid);
                        partsList.add(currentIndex, currentPart);
                    } else if (listPart == null && mapPart != null) {
                        currentPart = mapPart;
                        partsList.add(currentIndex, mapPart);
                    } else if (listPart != null && mapPart == null) {
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
                    ByteArrayBuffer buf = content.getData();
                    currentPart.addBody(buf);
                    break;

                case END_PART :
                    currentPart.doneParsing();
                    ++currentIndex;
                    break;

                case END_MESSAGE :
                    parsed = true;
                    break;

                default :
                    throw new MIMEParsingException("Unknown Parser state = "+event.getEventType());
            }
        }
    }

    void readMinimum(String contentId) {

    }

    void readHeaders(String contentId) {

    }

    void readNextBody(String contentId) {

    }
}
