/*
 * Copyright 2013-2019 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

/**
 * Exception thrown when watcher problems occur.
 * 
 * @see FileWatchService#watch(List, FileWatchCallback)
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class FileWatchException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message exception detail message
     */
    public FileWatchException( String message )
    {
        super( message );
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message exception detail message
     * @param cause exception cause (nested throwable)
     */
    public FileWatchException( String message, Throwable cause )
    {
        super( message, cause );
    }

}
