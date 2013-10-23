/*
 * Copyright 2013 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface Play2JavaEnhancer
{
    void setAnalysisCacheFile( File analysisCacheFile );

    void setClasspathFiles( List<File> classpathFiles );

    long getCompilationTime( File sourceFile );

    Set<File> getProducts( File sourceFile );

    void enhanceJavaClass( File classFile )
        throws Exception;

    void enhanceTemplateClass( File classFile )
        throws Exception;

}
