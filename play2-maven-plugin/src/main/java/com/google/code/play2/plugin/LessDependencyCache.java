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

package com.google.code.play2.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class LessDependencyCache
{
    private static final String FILE_HEADER = "format: 1";
    private static final String FILE_DEPENDENCY_PREFIX = "  ";

    private Map<String, Set<String>> allDependencies = new TreeMap<String, Set<String>>();

    public void readFromFile( File file )
        throws IOException
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ) );
        try
        {
            String line = reader.readLine();
            if ( !FILE_HEADER.equals( line ) )
            {
                
            }
            //check header
            line = reader.readLine();
            while ( line != null )
            {
                String fileName = line;
                Set<String> dependencies = new TreeSet<String>();
                line = reader.readLine();
                while ( line != null && line.startsWith( FILE_DEPENDENCY_PREFIX ) )
                {
                    String dependencyFileName = line.substring( 2 );
                    dependencies.add( dependencyFileName );
                    line = reader.readLine();
                }
                allDependencies.put( fileName, dependencies );
            }
        }
        finally
        {
            reader.close();
        }
    }

    public void writeToFile( File file )
        throws IOException
    {
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" ) );
        try
        {
            writer.write( FILE_HEADER );
            writer.newLine();
            for ( String fileName: allDependencies.keySet() )
            {
                writer.write( fileName );
                writer.newLine();
                Set<String> deps = allDependencies.get( fileName );
                for ( String dependencyFileName: deps )
                {
                    writer.write( FILE_DEPENDENCY_PREFIX );
                    writer.write( dependencyFileName );
                    writer.newLine();
                }
                
            }
        }
        finally
        {
            writer.flush();
            writer.close();
        }
    }

    public Set<String> get( String fileName )
    {
        return allDependencies.get( fileName );
    }
//    public Set<String> get( File file )
//    {
//        return allDependencies.get( file.getAbsolutePath() );
//    }

    public void set( String fileName, Collection<String> dependencies )
    {
        allDependencies.put( fileName, new TreeSet<String>( dependencies ) );
    }
//    public void set( File file, Collection<File> dependencies )
//    {
//        Set<String> deps = new TreeSet<String>();
//        for ( File dependencyFile: dependencies )
//        {
//            deps.add( dependencyFile.getAbsolutePath() );
//        }
//        allDependencies.put( file.getAbsolutePath(), deps );
//    }

    @Override
    public int hashCode()
    {
        return allDependencies.hashCode();
    }

    @Override
    public boolean equals( Object obj )
    {
        return obj instanceof LessDependencyCache && allDependencies.equals( ( (LessDependencyCache) obj ).allDependencies );
    }
    //TEMP
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( '\n' );
        for ( String key: allDependencies.keySet() )
        {
            //sb.append( String.format( "%s\n", key ) );
            sb.append( key );
            sb.append( '\n' );
            Set<String> deps = allDependencies.get( key );
            for ( String dep: deps )
            {
                sb.append( String.format( "  %s\n", dep ) );
            }
            
        }
        return sb.toString();
    }
}
