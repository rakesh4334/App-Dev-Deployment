package org.apache.maven.scm.provider.accurev;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Date;

import org.codehaus.plexus.util.StringUtils;

/**
 * 
 */
public class AccuRevVersion
{

    private String basisStream;

    private String timeSpec;

    public AccuRevVersion( String basisStream, String tran )
    {

        this.basisStream = basisStream;
        this.timeSpec = tran;
    }

    public String getBasisStream()
    {
        return basisStream;
    }

    public String getTimeSpec()
    {
        return timeSpec;
    }

    public AccuRevVersion( String basis, Date startDate )
    {
        this( basis, AccuRev.ACCUREV_TIME_SPEC.format( startDate ) );
    }

    public AccuRevVersion( String basis, long transactionId )
    {
        this( basis, Long.toString( transactionId ) );
    }

    public boolean isNow()
    {
        return isNow( this.timeSpec );
    }

    @Override
    public String toString()
    {
        return String.format( "AccuRevVersion: stream = %s, transaction= %s", basisStream, timeSpec );
    }

    public static boolean isNow( String timeSpec )
    {
        return StringUtils.isBlank( timeSpec ) || "highest".equalsIgnoreCase( timeSpec )
            || "now".equalsIgnoreCase( timeSpec );
    }
}
