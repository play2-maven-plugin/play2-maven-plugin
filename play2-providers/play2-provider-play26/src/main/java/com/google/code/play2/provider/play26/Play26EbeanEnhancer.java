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

package com.google.code.play2.provider.play26;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.ebean.enhance.Transformer;
import io.ebean.enhance.ant.StringReplace;
import io.ebean.enhance.common.InputStreamTransform;

import com.google.code.play2.provider.api.Play2EbeanEnhancer;

public class Play26EbeanEnhancer
    implements Play2EbeanEnhancer
{
    private static final String configLoaderClassName = "play.db.ebean.ModelsConfigLoader";

    private File outputDirectory;

    private List<URL> classPathUrls;

    private InputStreamTransform inputStreamTransform;

    @Override
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void setClassPathUrls( List<URL> classPathUrls )
    {
        this.classPathUrls = classPathUrls;

        URL[] cp = classPathUrls.toArray( new URL[classPathUrls.size()] );
        ClassLoader classLoader = new URLClassLoader( cp, null );
        Transformer transformer = new Transformer( classLoader, "debug=-1" );
        this.inputStreamTransform = new InputStreamTransform( transformer, classLoader );
    }

    @Override
    public String getModelsToEnhance()
    {
        Map<String, List<String>> config = getConfig();

        if ( !config.isEmpty() )
        {
            List<String> result = new ArrayList<String>();
            for ( List<String> modelsList: config.values() )
            {
                for ( String models: modelsList )
                {
                    if ( !result.contains( models ) )
                    {
                        result.add( models );
                    }
                }
            }
            return String.join( ",", result );
        }
        return  "models.*";
    }

    private Map<String, List<String>> getConfig()
    {
        URL[] cp = classPathUrls.toArray( new URL[classPathUrls.size()] );
        URLClassLoader classLoader = new URLClassLoader( cp, null );
        try
        {
            Class<?> configLoaderClass = classLoader.loadClass( configLoaderClassName );
            Function<ClassLoader, Map<String, List<String>>> configLoader =
                    ( Function<ClassLoader, Map<String, List<String>>> ) configLoaderClass.newInstance();

            return configLoader.apply( classLoader );
        }
        catch ( ClassNotFoundException | IllegalAccessException | InstantiationException e )
        {
            throw cloneException( e );
        }
        finally
        {
            try
            {
                classLoader.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    private RuntimeException cloneException( Throwable t )
    {
        RuntimeException cloned = new RuntimeException( t.getClass().getName() + ": " + t.getMessage() );
        cloned.setStackTrace( t.getStackTrace() );
        if ( t.getCause() != null )
        {
            cloned.initCause( cloneException( t.getCause() ) );
        }
        return cloned;
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
