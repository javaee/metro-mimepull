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

/**
 * @author Jitendra Kotamraju
 */
public class MIMEConfig {

    // Parses the entire message eagerly
    boolean parseEagerly;

    // Approximate Chunk size
    int chunkSize;

    // Maximum in-memory data per attachment
    int inMemorySize;

    // Do not store to disk
    boolean onlyMemory;

    // temp Dir to store large files
    String dir;

    private MIMEConfig(boolean parseEagerly, int chunkSize, int inMemorySize, boolean onlyMemory, String dir) {
        this.parseEagerly = parseEagerly;
        this.chunkSize = chunkSize;
        this.inMemorySize = inMemorySize;
        this.onlyMemory = onlyMemory;
        this.dir = dir;
    }

    public MIMEConfig() {
        this(false, 8192, 1048576, false, null);
    }

    public boolean isParseEagerly() {
        return parseEagerly;
    }

    public void setParseEagerly(boolean parseEagerly) {
        this.parseEagerly = parseEagerly;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getInMemorySize() {
        return inMemorySize;
    }

    public void setInMemorySize(int inMemorySize) {
        this.inMemorySize = inMemorySize;
    }

    public boolean isOnlyMemory() {
        return onlyMemory;
    }

    public void setOnlyMemory(boolean onlyMemory) {
        this.onlyMemory = onlyMemory;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

}
