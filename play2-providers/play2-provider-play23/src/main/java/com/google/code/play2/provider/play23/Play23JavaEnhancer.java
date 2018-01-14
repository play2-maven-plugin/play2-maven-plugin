/*
 * Copyright 2013-2018 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play23;

import java.io.File;
import java.util.List;

import play.core.enhancers.PropertiesEnhancer;

import com.google.code.play2.provider.api.Play2JavaEnhancer;

public class Play23JavaEnhancer
    implements Play2JavaEnhancer
{
    private String classpath;

    @Override
    public void setClasspathFiles( List<File> classpathFiles )
    {
        StringBuilder sb = new StringBuilder();
        for ( File classpathFile : classpathFiles )
        {
            sb.append( File.pathSeparatorChar );
            sb.append( classpathFile.getAbsolutePath() );
        }
        this.classpath = sb.substring( 1 );
    }

    @Override
    public boolean generateAccessors( File classFile )
        throws Exception
    {
        return PropertiesEnhancer.generateAccessors( classpath, classFile );
    }

    @Override
    public boolean rewriteAccess( File classFile )
        throws Exception
    {
        return PropertiesEnhancer.rewriteAccess( classpath, classFile );
    }

}
