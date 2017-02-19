/*
 * Copyright 2013-2017 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play26;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.google.code.play2.provider.api.Play2EbeanEnhancer;

import com.avaje.ebean.enhance.agent.InputStreamTransform;
import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.ant.StringReplace;

public class Play26EbeanEnhancer
    implements Play2EbeanEnhancer
{
    private File outputDirectory;

    private InputStreamTransform inputStreamTransform;

    @Override
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void setClassPathUrls( List<URL> classPathUrls )
    {
        URL[] cp = classPathUrls.toArray( new URL[classPathUrls.size()] );
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Transformer transformer = new Transformer( cp, "debug=-1" );
        inputStreamTransform = new InputStreamTransform( transformer, classLoader );
    }

    @Override
    public boolean enhanceModel( File classFile )
        throws Exception
    {
        boolean processed = false;
        
        String className = getClassName( classFile );
        byte[] result = inputStreamTransform.transform( className, classFile );
        if ( result != null )
        {
            InputStreamTransform.writeBytes( result, classFile );
            processed = true;
        }
        return processed;
    }

    private String getClassName( File file )
    {
        String path = file.getPath();
        path = path.substring( outputDirectory.getAbsolutePath().length() + 1 );
        path = path.substring( 0, path.length() - ".class".length() );
        // for windows... replace the
        return StringReplace.replace( path, "\\", "/" );
    }

}
