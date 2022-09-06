package org.apache.maven.scm.provider.svn.svnexe.command;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.util.SvnUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Command line construction utility.
 *
 * @author Brett Porter
 * @author Olivier Lamy
 *
 */
public final class SvnCommandLineUtils
{
    private SvnCommandLineUtils()
    {
    }

    public static void addTarget( Commandline cl, List<File> files )
        throws IOException
    {
        if ( files == null || files.isEmpty() )
        {
            return;
        }

        StringBuilder sb = new StringBuilder();
        String ls = System.getProperty( "line.separator" );
        for ( File f : files )
        {
            sb.append( f.getPath().replace( '\\', '/' ) );
            sb.append( ls );
        }

        File targets = File.createTempFile( "maven-scm-", "-targets" );
        PrintStream out = new PrintStream( new FileOutputStream( targets ) );
        out.print( sb.toString() );
        out.flush();
        out.close();

        cl.createArg().setValue( "--targets" );
        cl.createArg().setValue( targets.getAbsolutePath() );

        targets.deleteOnExit();
    }

    public static Commandline getBaseSvnCommandLine( File workingDirectory, SvnScmProviderRepository repository )
    {
        Commandline cl = new Commandline();

        cl.setExecutable( "svn" );
        try
        {
            cl.addSystemEnvironment();
            cl.addEnvironment( "LC_MESSAGES", "C" );
        }
        catch ( Exception e )
        {
            //Do nothing
        }

        if ( workingDirectory != null )
        {
            cl.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        }

        if ( !StringUtils.isEmpty( System.getProperty( "maven.scm.svn.config_directory" ) ) )
        {
            cl.createArg().setValue( "--config-dir" );
            cl.createArg().setValue( System.getProperty( "maven.scm.svn.config_directory" ) );
        }
        else if ( !StringUtils.isEmpty( SvnUtil.getSettings().getConfigDirectory() ) )
        {
            cl.createArg().setValue( "--config-dir" );
            cl.createArg().setValue( SvnUtil.getSettings().getConfigDirectory() );
        }

        boolean hasAuthInfo = false;
        if ( repository != null && !StringUtils.isEmpty( repository.getUser() ) )
        {
            hasAuthInfo = true;
            cl.createArg().setValue( "--username" );
            cl.createArg().setValue( repository.getUser() );
        }

        if ( repository != null && !StringUtils.isEmpty( repository.getPassword() ) )
        {
            hasAuthInfo = true;
            cl.createArg().setValue( "--password" );
            cl.createArg().setValue( repository.getPassword() );
        }

        // [by Lenik] don't overwrite existing auth cache by default.
        if ( hasAuthInfo && !SvnUtil.getSettings().isUseAuthCache() )
        {
            cl.createArg().setValue( "--no-auth-cache" );
        }

        if ( SvnUtil.getSettings().isUseNonInteractive() )
        {
            cl.createArg().setValue( "--non-interactive" );
        }

        if ( SvnUtil.getSettings().isTrustServerCert() )
        {
            cl.createArg().setValue( "--trust-server-cert" );
        }

        return cl;
    }

    public static int execute( Commandline cl, StreamConsumer consumer, CommandLineUtils.StringStreamConsumer stderr,
                               ScmLogger logger )
        throws CommandLineException
    {
        // SCM-482: force English resource bundle
        cl.addEnvironment( "LC_MESSAGES", "en" );

        int exitCode = CommandLineUtils.executeCommandLine( cl, consumer, stderr );

        exitCode = checkIfCleanUpIsNeeded( exitCode, cl, consumer, stderr, logger );

        return exitCode;
    }

    public static int execute( Commandline cl, CommandLineUtils.StringStreamConsumer stdout,
                               CommandLineUtils.StringStreamConsumer stderr, ScmLogger logger )
        throws CommandLineException
    {
        int exitCode = CommandLineUtils.executeCommandLine( cl, stdout, stderr );

        exitCode = checkIfCleanUpIsNeeded( exitCode, cl, stdout, stderr, logger );

        return exitCode;
    }

    private static int checkIfCleanUpIsNeeded( int exitCode, Commandline cl, StreamConsumer consumer,
                                               CommandLineUtils.StringStreamConsumer stderr, ScmLogger logger )
        throws CommandLineException
    {
        if ( exitCode != 0 && stderr.getOutput() != null && stderr.getOutput().indexOf( "'svn cleanup'" ) > 0
            && stderr.getOutput().indexOf( "'svn help cleanup'" ) > 0 )
        {
            if ( logger.isInfoEnabled() )
            {
                logger.info( "Svn command failed due to some locks in working copy. We try to run a 'svn cleanup'." );
            }

            if ( executeCleanUp( cl.getWorkingDirectory(), consumer, stderr, logger ) == 0 )
            {
                exitCode = CommandLineUtils.executeCommandLine( cl, consumer, stderr );
            }
        }
        return exitCode;
    }

    public static int executeCleanUp( File workinDirectory, StreamConsumer stdout, StreamConsumer stderr )
        throws CommandLineException
    {
        return executeCleanUp( workinDirectory, stdout, stderr, null );
    }

    public static int executeCleanUp( File workinDirectory, StreamConsumer stdout, StreamConsumer stderr,
                                      ScmLogger logger )
        throws CommandLineException
    {
        Commandline cl = new Commandline();

        cl.setExecutable( "svn" );

        cl.setWorkingDirectory( workinDirectory.getAbsolutePath() );

        if ( logger != null )
        {
            if ( logger.isInfoEnabled() )
            {
                logger.info( "Executing: " + SvnCommandLineUtils.cryptPassword( cl ) );

                if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
                {
                    logger.info( "Working directory: " + cl.getWorkingDirectory().getAbsolutePath() );
                }
            }
        }

        return CommandLineUtils.executeCommandLine( cl, stdout, stderr );
    }

    public static String cryptPassword( Commandline cl )
    {
        String clString = cl.toString();

        final String passwordOpt = "--password";
        String quoteChar;
        String escapedQuoteChar;
        String cryptedPassword;

        int pos = clString.indexOf( passwordOpt );

        if ( pos > 0 )
        {
           if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
           {
                quoteChar = "\"";
                escapedQuoteChar = "\"\"";
                cryptedPassword = "*****";
           }
           else
           {
               quoteChar = "'";
               escapedQuoteChar = "'\"'\"'";
               cryptedPassword = "'*****'";
           }

           // Move pointer after password option
           pos += passwordOpt.length();

           // Skip quote after password option
           if ( clString.substring( pos,  pos + 1 ).equals( quoteChar ) )
           {
               pos++;
           }

           // Skip space after password option
           pos++;

            String beforePassword = clString.substring( 0, pos );
            String afterPassword = clString.substring( pos );

            if ( afterPassword.startsWith( quoteChar ) )
            {
                pos = 1;
                while ( afterPassword.indexOf( escapedQuoteChar, pos ) != -1 )
                {
                    pos = afterPassword.indexOf( escapedQuoteChar, pos ) + escapedQuoteChar.length();
                }
                afterPassword = afterPassword.substring ( afterPassword.indexOf( quoteChar, pos )
                                                          + quoteChar.length() );
            }
            else
            {
                // We assume that the password arg ist not the last one on the arg list
                afterPassword = afterPassword.substring( afterPassword.indexOf( ' ' ) );
            }

            clString = beforePassword + cryptedPassword + afterPassword;

        }

        return clString;
    }
}
