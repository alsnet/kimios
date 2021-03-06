/*
 * Kimios - Document Management System Software
 * Copyright (C) 2008-2015  DevLib'
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * aong with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kimios.client.controller;

import org.kimios.client.exception.ExceptionHelper;
import org.kimios.kernel.reporting.model.Report;
import org.kimios.kernel.reporting.model.ReportParam;
import org.kimios.webservices.ReportingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReportingController is used to call the reporting functionalities
 *
 * @author jludmann
 */
public class ReportingController
{

    private ReportingService client;

    public ReportingService getClient()
    {
        return client;
    }

    public void setClient( ReportingService client )
    {
        this.client = client;
    }

    /**
     * Return specific report from given XML parameters
     */
    public String getReport( String sessionId, String className, String xmlParameters )
        throws Exception
    {
        try
        {
            return client.getReport( sessionId, className, xmlParameters );
        }
        catch ( Exception e )
        {
            throw new ExceptionHelper().convertException( e );
        }
    }

    /**
     * Return specific report from given parameters
     */
    public String getReport(String sessionId, String className, Map<String, ReportParam> reportParams)
            throws Exception
    {
        try
        {
            List<ReportParam> items = new ArrayList<ReportParam>();
            items.addAll(reportParams.values());
            return client.getReport( sessionId, className, items );
        }
        catch ( Exception e )
        {
            throw new ExceptionHelper().convertException( e );
        }
    }

    /**
     * Return XML stream containing all attributes from a specified report with
     * its class name
     */
    @Deprecated
    public String getReportAttributesXml( String sessionId, String className )
        throws Exception
    {
        try
        {
            return client.getReportAttributesXml( sessionId, className );
        }
        catch ( Exception e )
        {
            throw new ExceptionHelper().convertException( e );
        }
    }



    /**
     * Return list containing all attributes from a specified report with
     * its class name
     */
    public List<ReportParam> getReportAttributes(String sessionId, String className )
            throws Exception
    {
        try
        {
            return client.getReportAttributes( sessionId, className );
        }
        catch ( Exception e )
        {
            throw new ExceptionHelper().convertException( e );
        }
    }

    /**
     * Return XML stream containing all implemented reports
     */
    public String getReportsListXml( String sessionId )
        throws Exception
    {
        try
        {
            return client.getReportsListXml( sessionId );
        }
        catch ( Exception e )
        {
            throw new ExceptionHelper().convertException( e );
        }
    }

    /**
     * Return Stream containing all implemented reports
     */
    public List<Report> getReportsList(String sessionId )
            throws Exception
    {
        try
        {
            return client.getReportsList( sessionId );
        }
        catch ( Exception e )
        {
            throw new ExceptionHelper().convertException( e );
        }
    }

    /**
     * Clear uncompleted transactions
     */
    public void removeGhostTransaction( String sessionId, Long transactionId )
        throws Exception
    {
        try
        {
            client.removeGhostTransaction( sessionId, transactionId );
        }
        catch ( Exception ex )
        {
            throw new ExceptionHelper().convertException( ex );
        }
    }
}

