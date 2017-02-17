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

package com.google.code.play2.provider.play23.run;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import play.core.BuildLink;

import com.google.code.play2.provider.api.Play2Builder;
import com.google.code.play2.provider.api.Play2BuildError;
import com.google.code.play2.provider.api.Play2BuildFailure;

public class Reloader implements BuildLink
{
    private Play2Builder buildLink;

    private ClassLoader baseLoader;

    private File projectPath;

    private List<File> outputDirectories;

    private Map<String, String> devSettings;

    private volatile ClassLoader currentApplicationClassLoader = null;

    private int classLoaderVersion = 0;

    public Reloader( Play2Builder buildLink, ClassLoader baseLoader, File projectPath, List<File> outputDirectories,
                     Map<String, String> devSettings )
    {
        this.buildLink = buildLink;
        this.baseLoader = baseLoader;
        this.projectPath = projectPath;
        this.outputDirectories = outputDirectories;
        this.devSettings = devSettings;
    }
    /**
     * Contrary to its name, this doesn't necessarily reload the app.  It is invoked on every request, and will only
     * trigger a reload of the app if something has changed.
     *
     * Since this communicates across classloaders, it must return only simple objects.
     *
     *
     * @return Either
     * - Throwable - If something went wrong (eg, a compile error).
     * - ClassLoader - If the classloader has changed, and the application should be reloaded.
     * - null - If nothing changed.
     */
    @Override /* BuildLink interface */
    public synchronized Object reload()
    {
        Object result = null;
        try
        {
            boolean reloadRequired = buildLink.build();

            if ( reloadRequired )
            {
                int version = ++classLoaderVersion;
                String name = "ReloadableClassLoader(v" + version + ")";
                currentApplicationClassLoader =
                    new DelegatedResourcesClassLoader( name, toUrls( outputDirectories ), baseLoader );
                result = currentApplicationClassLoader;
            }
        }
        catch ( MalformedURLException e )
        {
            throw new UnexpectedException( "Unexpected reloader exception", e ); //??
        }
        catch ( Play2BuildFailure e )
        {
            result =
                new CompilationException( e.getMessage(), e.line(), e.position(),
                                          e.source() != null ? e.source().getAbsolutePath() : null, e.input() );
        }
        catch ( Play2BuildError e )
        {
            result = new UnexpectedException( e.getMessage(), e.getCause() ); //??
        }
        return result;
    }

    @Override /* BuildLink interface */
    public Map<String, String> settings()
    {
        return devSettings;
    }

    @Override /* BuildLink interface */
    public void forceReload()
    {
        // NOT USED
    }

    @Override /* BuildLink interface */
    public Object[] findSource( String className, Integer line )
    {
        return buildLink.findSource( className, line );
    }

    @Override /* BuildLink interface */
    public Object runTask( String task )
    {
        return null; // NOT USED
    }

    @Override /* BuildLink interface */
    public File projectPath()
    {
        return projectPath;
    }

    void close()
    {
        currentApplicationClassLoader = null;
    }

    ClassLoader getClassLoader()
    {
        return currentApplicationClassLoader;
    }

    public static URL[] toUrls( List<File> cp ) throws MalformedURLException
    {
        URL[] result = new URL[cp.size()];
        for ( int i = 0; i < cp.size(); i++ )
        {
            File file = cp.get( i );
            result[i] = file.toURI().toURL();
        }
        return result;
    }

}
