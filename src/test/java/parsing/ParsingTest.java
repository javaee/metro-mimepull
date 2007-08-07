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
package parsing;

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.List;

import org.jvnet.mimepull.MIMEMessage;
import org.jvnet.mimepull.MIMEConfig;
import org.jvnet.mimepull.MIMEPart;


/**
 * @author Jitendra Kotamraju
 */
public class ParsingTest extends TestCase {

    public void testMsg() throws Exception {
        InputStream in = getClass().getResourceAsStream("../msg.txt");
        String boundary = "------=_Part_4_910054940.1065629194743";
        MIMEConfig config = new MIMEConfig(false, 1024, 512);
        MIMEMessage mm = new MIMEMessage(in, boundary , config);
        mm.parseAll();
        List<MIMEPart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("139912840220.1065629194743.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(0).getContentId());
        assertEquals("1351327060508.1065629194423.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(1).getContentId());
    }

    public void testMsg2() throws Exception {
        InputStream in = getClass().getResourceAsStream("../msg2.txt");
        String boundary = "------=_Part_1_807283631.1066069460327";
        MIMEConfig config = new MIMEConfig(false, 1024, 512);
        MIMEMessage mm = new MIMEMessage(in, boundary , config);
        mm.parseAll();
        List<MIMEPart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("1071294019496.1066069460327.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(0).getContentId());
        assertEquals("871169419176.1066069460266.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(1).getContentId());
    }

    public void testMessage1() throws Exception {
        InputStream in = getClass().getResourceAsStream("../message1.txt");
        String boundary = "------=_Part_7_10584188.1123489648993";
        MIMEConfig config = new MIMEConfig(false, 1024, 512);
        MIMEMessage mm = new MIMEMessage(in, boundary , config);
        mm.parseAll();
        List<MIMEPart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("soapPart", parts.get(0).getContentId());
        assertEquals("attachmentPart", parts.get(1).getContentId());
    }

}
