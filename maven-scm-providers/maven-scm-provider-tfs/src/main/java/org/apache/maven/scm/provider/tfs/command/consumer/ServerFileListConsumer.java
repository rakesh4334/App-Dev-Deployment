package org.apache.maven.scm.provider.tfs.command.consumer;

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

import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileStatus;
import org.codehaus.plexus.util.StringUtils;

/**
 * 
 */
public class ServerFileListConsumer
    extends FileListConsumer
{
    protected ScmFile getScmFile( String filename )
    {
        if ( filename.startsWith( "$" ) )
        {
            filename = StringUtils.replace( filename, "$", "", -1 );
            //filename = filename.replace( "$", "" );
        }
        String path = currentDir + "/" + filename;
        return new ScmFile( path, ScmFileStatus.UNKNOWN );
    }
}
