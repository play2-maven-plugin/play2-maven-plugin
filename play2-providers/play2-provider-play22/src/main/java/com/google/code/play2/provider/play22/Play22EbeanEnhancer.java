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

package com.google.code.play2.provider.play22;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import com.google.code.play2.provider.Play2EbeanEnhancer;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.ant.OfflineFileTransform;

public class Play22EbeanEnhancer
    implements Play2EbeanEnhancer
{
    private File outputDirectory;

    private List<URL> classPathUrls = Collections.emptyList();

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setClassPathUrls( List<URL> classPathUrls )
    {
        this.classPathUrls = classPathUrls;
    }

    public void enhance( File applicationConfFile ) // what about exceptions?
    {
        URL[] cp = classPathUrls.toArray( new URL[classPathUrls.size()] );

        ClassLoader cl = ClassLoader.getSystemClassLoader();

        Transformer t = new Transformer( cp, "debug=-1" );

        OfflineFileTransform ft = new OfflineFileTransform( t, cl, /* classes */outputDirectory.getAbsolutePath(), /* classes */
        outputDirectory.getAbsolutePath() );

        Config config = ConfigFactory.load( ConfigFactory.parseFileAnySyntax( applicationConfFile ) );

        String models = null;
        try
        {
            // see https://github.com/playframework/Play20/wiki/JavaEbean
            Set<Map.Entry<String, ConfigValue>> entries = config.getConfig( "ebean" ).entrySet();
            for ( Map.Entry<String, ConfigValue> entry : entries )
            {
                ConfigValue configValue = entry.getValue();
                Object configValueUnwrapped = configValue.unwrapped();
                // TODO-optimize
                if ( models == null )
                {
                    models = configValueUnwrapped.toString();
                }
                else
                {
                    models = models + "," + configValueUnwrapped.toString();
                }
            }
        }
        catch ( ConfigException.Missing e )
        {
            models = "models.*";
        }

        try
        {
            ft.process( models );
        }
        catch ( Throwable/* ? */e )
        {

        }
    }

}
