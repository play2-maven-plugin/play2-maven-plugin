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

package com.google.code.play2.provider.play22;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.enhance.agent.InputStreamTransform;
import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.ant.StringReplace;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;

import com.google.code.play2.provider.api.Play2EbeanEnhancer;

public class Play22EbeanEnhancer
    implements Play2EbeanEnhancer
{
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
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Transformer transformer = new Transformer( cp, "debug=-1" );
        this.inputStreamTransform = new InputStreamTransform( transformer, classLoader );
    }

    @Override
    public String getModelsToEnhance() //TODO - change to collection of strings
    {
        Config config = getConfig();
        try
        {
            StringBuilder collector = new StringBuilder();

            Set<Map.Entry<String, ConfigValue>> entries = config.getConfig( "ebean" ).entrySet();
            for ( Map.Entry<String, ConfigValue> entry : entries )
            {
                ConfigValue configValue = entry.getValue();
                collector.append( ',' ).append( configValue.unwrapped().toString() );
            }
            return collector.length() != 0 ? collector.substring( 1 ) : null;
        }
        catch ( ConfigException.Missing e )
        {
            return "models.*";
        }
    }

    private Config getConfig()
    {
        String configResource = System.getProperty( "config.resource" );
        if ( configResource != null )
        {
            URL[] cp = classPathUrls.toArray( new URL[classPathUrls.size()] );
            URLClassLoader classLoader = new URLClassLoader( cp, null );
            ConfigParseOptions options = ConfigParseOptions.defaults().setClassLoader( classLoader );
            return ConfigFactory.parseResources( configResource, options );
        }
        String configFileName = System.getProperty( "config.file" );
        if ( configFileName != null )
        {
            return ConfigFactory.parseFileAnySyntax( new File( configFileName ) );
        }
        // in 'process-classes' phase resources are already copied to the output directory
        return ConfigFactory.parseFileAnySyntax( new File( outputDirectory, "application.conf" ) );
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
