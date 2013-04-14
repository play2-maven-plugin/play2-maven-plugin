/*
 * Copyright 2012 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.less;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LessDependencyCache
{
    private Map<String, Set<String>> allDependencies = new HashMap<String, Set<String>>();

    public void readFromFile( File file )
        throws IOException
    {
        try
        {
            allDependencies = (Map<String, Set<String>>) deserializeObjectFromFile( file );
        }
        catch ( ClassNotFoundException e )
        {
            // ???throw new MojoExecutionException( "?", e );
        }
    }

    public void writeToFile( File file )
        throws IOException
    {
        serializeObjectToFile( allDependencies, file );
    }

    public Set<String> get( String fileName )
    {
        return allDependencies.get( fileName );
    }

    public void set( String fileName, Set<String> dependencies )
    {
        allDependencies.put( fileName, dependencies );
    }

    private void serializeObjectToFile( Object object, File file )
        throws IOException
    {
        serializeObjectToStream( object, new FileOutputStream( file ) );
    }

    private void serializeObjectToStream( Object object, OutputStream outputStream )
        throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream( outputStream );
        try
        {
            oos.writeObject( object );
        }
        finally
        {
            oos.flush();
            oos.close();// GS-czy tutaj?
        }
    }

    private Object deserializeObjectFromFile( File file )
        throws ClassNotFoundException, IOException
    {
        return deserializeObjectFromStream( new FileInputStream( file ) );
    }

    private Object deserializeObjectFromStream( InputStream inputStream )
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream ois = new ObjectInputStream( inputStream );
        try
        {
            return ois.readObject();
        }
        finally
        {
            ois.close();// GS-czy tutaj?
        }
    }

}
