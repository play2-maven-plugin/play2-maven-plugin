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
 * Logger.
 *
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public interface FileWatchLogger
{
    /**
     * Returns true if <b>debug</b> log level is enabled.
     * 
     * @return true if <b>debug</b> log level is enabled
     */
    boolean isDebugEnabled();

    /**
     * Sends a message in <b>debug</b> log level.
     *
     * @param content debug message
     */
    void debug( String content );

    /**
     * Sends a throwable in the <b>debug</b> log level.
     * <br>
     * The stack trace for this throwable will be output.
     *
     * @param throwable debug throwable
     */
    void debug( Throwable throwable );

    /**
     * Returns true if <b>info</b> log level is enabled.
     * 
     * @return true if <b>info</b> log level is enabled
     */
    boolean isInfoEnabled();

    /**
     * Sends a message in <b>info</b> log level.
     *
     * @param content info message
     */
    void info( String content );

    /**
     * Returns true if <b>warn</b> log level is enabled.
     * 
     * @return true if <b>warn</b> log level is enabled
     */
    boolean isWarnEnabled();

    /**
     * Sends a message in <b>warn</b> log level.
     *
     * @param content warning message
     */
    void warn( String content );

    /**
     * Returns true if <b>error</b> log level is enabled.
     * 
     * @return true if <b>error</b> log level is enabled
     */
    boolean isErrorEnabled();

    /**
     * Sends a message in the <b>error</b> log level.
     *
     * @param content error message
     */
    void error( String content );

}