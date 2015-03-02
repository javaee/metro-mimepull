/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.util.List;

/**
 * @author Martin Grebac
 */
public class QuotedTest extends TestCase {

    private static final String PATH = "../../../";

    public void testMsg() throws Exception {
        InputStream in = getClass().getResourceAsStream(PATH + "quoted.txt");
        String boundary = "----=_Part_16_799571960.1350659465464";
        MIMEConfig config = new MIMEConfig();
        MIMEMessage mm = new MIMEMessage(in, boundary , config);
        mm.parseAll();
        
        List<MIMEPart> parts = mm.getAttachments();
        MIMEPart part1 = parts.get(1);

        assertTrue(part1.getContentTransferEncoding().equals("quoted-printable"));
        
        InputStream is = part1.readOnce();
        byte[] buf = new byte[8192];
        int len = is.read(buf, 0, buf.length);        
        String str = new  String(buf, 0, len);
        
        assertFalse(str.contains("=3D"));
        
        part1.close();
    }

}
