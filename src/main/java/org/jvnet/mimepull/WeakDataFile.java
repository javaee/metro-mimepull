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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Removing files based on this
 * <a href="http://java.sun.com/developer/technicalArticles/javase/finalization/">article</a>
 *
 * @author Jitendra Kotamraju
 */
final class WeakDataFile extends WeakReference<DataFile> {

    private static final Logger LOGGER = Logger.getLogger(WeakDataFile.class.getName());
    private static int TIMEOUT = 10; //milliseconds
    //private static final int MAX_ITERATIONS = 2;
    private static ReferenceQueue<DataFile> refQueue = new ReferenceQueue<DataFile>();
    private static List<WeakDataFile> refList = new ArrayList<WeakDataFile>();
    private final File file;
    private final RandomAccessFile raf;
    private static boolean hasCleanUpExecutor = false;
    static {
    	int delay = 10;
    	try {
    		delay = Integer.getInteger("org.jvnet.mimepull.delay", 10);
    	} catch (SecurityException se) {
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.log(Level.CONFIG, "Cannot read ''{0}'' property, using defaults.",
                        new Object[] {"org.jvnet.mimepull.delay"});
            } 
    	}
        CleanUpExecutorFactory executorFactory = CleanUpExecutorFactory.newInstance();
        if (executorFactory!=null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Initializing clean up executor for MIMEPULL: {0}", executorFactory.getClass().getName());
            }
            ScheduledExecutorService scheduler = executorFactory.getScheduledExecutorService();
            scheduler.scheduleWithFixedDelay(new CleanupRunnable(), delay, delay, TimeUnit.SECONDS);
            hasCleanUpExecutor = true;
        }
    }

    WeakDataFile(DataFile df, File file) {
        super(df, refQueue);
        refList.add(this);
        this.file = file;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
        if (!hasCleanUpExecutor) {
            drainRefQueueBounded();
        }
    }

    synchronized void read(long pointer, byte[] buf, int offset, int length ) {
        try {
            raf.seek(pointer);
            raf.readFully(buf, offset, length);
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    synchronized long writeTo(long pointer, byte[] data, int offset, int length) {
        try {
            raf.seek(pointer);
            raf.write(data, offset, length);
            return raf.getFilePointer();    // Update pointer for next write
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    void close() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Deleting file = {0}", file.getName());
        }
        refList.remove(this);
        try {
            raf.close();
            boolean deleted = file.delete();
            if (!deleted) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "File {0} was not deleted", file.getAbsolutePath());
                }
            }
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }
    }

    void renameTo(File f) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Moving file={0} to={1}", new Object[]{file, f});
        }
        refList.remove(this);
        try {
            raf.close();
            Path target = Files.move(file.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            boolean renamed = f.toPath().equals(target);
            if (!renamed) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    throw new MIMEParsingException("File " + file.getAbsolutePath() +
                            " was not moved to " + f.getAbsolutePath());
                }
            }
        } catch(IOException ioe) {
            throw new MIMEParsingException(ioe);
        }

    }

    static void drainRefQueueBounded() {
        WeakDataFile weak;
        while (( weak = (WeakDataFile) refQueue.poll()) != null ) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Cleaning file = {0} from reference queue.", weak.file);
            }
            weak.close();
        }
    }
    
private static class CleanupRunnable implements Runnable {
    @Override
    public void run() {
        try {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Running cleanup task");
            }
        	WeakDataFile weak = (WeakDataFile) refQueue.remove(TIMEOUT);
            while (weak != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Cleaning file = {0} from reference queue.", weak.file);
                }
                weak.close();
                weak = (WeakDataFile) refQueue.remove(TIMEOUT);
            }
        } catch (InterruptedException e) {
        }
    }
}    
}
