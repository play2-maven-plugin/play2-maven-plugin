/*
 * Copyright 2013-2020 Grzegorz Slowikowski (gslowikowski at gmail dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.google.code.play2.watcher.api;

import java.io.File;
import java.util.List;

/**
 * A service that can watch files.
 *
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public interface FileWatchService
{

    /**
     * Initializes watch service
     *
     * @param log logger
     * @throws FileWatchException when initialization fails
     */
    void initialize( FileWatchLogger log ) throws FileWatchException;

    /**
     * Watch the given sequence of files or directories.
     *
     * @param filesToWatch The files to watch.
     * @param onChange A callback that is executed whenever something changes.
     * @return A watcher
     * @throws FileWatchException when unexpected problems occur
     */
    FileWatcher watch( List<File> filesToWatch, FileWatchCallback onChange )
        throws FileWatchException;

}
