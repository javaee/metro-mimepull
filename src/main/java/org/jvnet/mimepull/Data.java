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

import java.nio.ByteBuffer;

/**
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
interface Data {

    /**
     * size of the chunk given by the parser
     *
     * @return size of the chunk
     */
    int size();

    /**
     * TODO: should the return type be ByteBuffer ??
     * Return part's partial data. The data is read only.
     *
     * @return a byte array which contains {#size()} bytes. The returned
     *         array may be larger than {#size()} bytes and contains data
     *         from offset 0.
     */
    byte[] read();

    /**
     * Write this partial data to a file
     *
     * @param file to which the data needs to be written
     * @return file pointer before the write operation(at which the data is
     *         written from)
     */
    long writeTo(DataFile file);

    /**
     * Factory method to create a Data. The implementation could
     * be file based one or memory based one.
     *
     * @param dataHead start of the linked list of data objects
     * @param buf contains partial content for a part
     * @return Data
     */
    Data createNext(DataHead dataHead, ByteBuffer buf);
}
