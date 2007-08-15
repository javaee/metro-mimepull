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

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Represents an attachment part in a MIME message. MIME message parsing is done
 * lazily using a pull parser, so the part may not have all the data. {@link #read}
 * and {@link #readOnce} may trigger the actual parsing the message. In fact,
 * parsing of an attachment part may be triggered by calling {@link #read} methods
 * on some other attachemnt parts. All this happens behind the scenes so the
 * application developer need not worry about these details.
 *
 * @author Jitendra Kotamraju
 */
public class MIMEPart {

    /**
     * Linked list to keep the part's content
     */
    Chunk head, tail;

    /**
     * If the part is stored in a file, non-null.
     *
     * If head is non-null, then we have the whole part in the file,
     * otherwise the file is only partial.
     */
    DataFile dataFile;

    private InternetHeaders headers;
    private String contentId;
    private String contentType;
    volatile boolean parsed;    // part is parsed or not
    private final MIMEMessage msg;
    private final MIMEConfig config;

    MIMEPart(MIMEMessage msg, MIMEConfig config) {
        this.msg = msg;
        this.config = config;
    }

    MIMEPart(MIMEMessage msg, MIMEConfig config, String contentId) {
        this(msg, config);
        this.contentId = contentId;
    }

    /**
     * Can get the attachment part's content multiple times. That means
     * the full content needs to be there in memory or on the file system.
     * Calling this method would trigger parsing for the part's data. So
     * do not call this unless it is required(otherwise, just wrap MIMEPart
     * into a object that returns InputStream for e.g DataHandler)
     *
     * @return data for the part's content
     */
    public InputStream read() {
        // Have the complete data on the file system
        if (parsed && dataFile != null && head != null) {
            return dataFile.getInputStream();
        }

        // Trigger parsing for the part
        while(tail == null) {
            if (!msg.makeProgress()) {
                throw new IllegalStateException("No such content ID: "+contentId);
            }
        }

        if (head == null) {
            throw new IllegalStateException("Already read. Probably readOnce() is called before.");
        }

        return new ChunkInputStream(msg, this, head);
    }
    

    /**
     * Can get the attachment part's content only once. The content
     * will be lost after the method. Content data is not be stored
     * on the file system or is not kept in the memory for the
     * following case:
     *   - Attachement parts contents are accessed sequentially
     *
     * In general, take advantage of this when the data is used only
     * once.
     *
     * @return data for the part's content
     */
    public InputStream readOnce() {
        InputStream in = read();
        head = null;
        return in;
    }

    public void moveTo(File f) {
        /*
        if(tail==null) {
            dataFile = new DataFile(f);
            tail = head = new Chunk(new FileData(dataFile));
        } else {
            if(head==null)
                throw new IllegalStateException("already read once");

            if(dataFile!=null) {
                dataFile.renameTo(f);
            } else {

            }
        }
        */
    }

    /**
     * Returns Content-ID MIME header for this attachment part
     *
     * @return Content-ID of the part
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Returns Content-Type MIME header for this attachment part
     *
     * @return Content-Type of the part
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Return all the values for the specified header.
     * Returns <code>null</code> if no headers with the
     * specified name exist.
     *
     * @param	name header name
     * @return	list of header values, or null if none
     */
    public List<String> getHeader(String name) {
        return headers.getHeader(name);
    }

    /**
     * Return all the headers
     *
     * @return list of Header objects
     */
    public List<? extends Header> getAllHeaders() {
        return headers.getAllHeaders();
    }

    /**
     * Callback to set headers
     *
     * @param headers MIME headers for the part
     */
    void setHeaders(InternetHeaders headers) {
        this.headers = headers;
        List<String> ct = getHeader("Content-Type");
        this.contentType = (ct == null) ? "application/octet-stream" : ct.get(0);
    }

    /**
     * Callback to notify that there is a partial content for the part
     *
     * @param buf content data for the part
     */
    void addBody(ByteBuffer buf) {
        if (tail!=null) {
            tail = tail.createNext(head, this, buf);
        } else {
            head = tail = new Chunk(new MemoryData(buf, config));
        }
    }

    /**
     * Callback to indicate that parsing is done for this part
     * (no more update events for this part)
     */
    void doneParsing() {
        parsed = true;
    }

    /**
     * Callback to set Content-ID for this part
     * @param cid Content-ID of the part
     */
    void setContentId(String cid) {
        this.contentId = cid;
    }

    @Override
    public String toString() {
        return "Part="+contentId;
    }

}
