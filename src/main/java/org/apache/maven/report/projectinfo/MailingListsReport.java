package org.apache.maven.report.projectinfo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Generates the Mailing Lists report.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @since 2.0
 */
@Mojo( name = "mailing-lists" )
public class MailingListsReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            result = !isEmpty( getProject().getModel().getMailingLists() );
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        MailingListsRenderer r =
            new MailingListsRenderer( getSink(), getProject().getModel(), getI18N( locale ), locale );

        r.render();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "mailing-lists";
    }

    @Override
    protected String getI18Nsection()
    {
        return "mailing-lists";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    protected static class MailingListsRenderer
        extends AbstractProjectInfoRenderer
    {
        private final Model model;

        MailingListsRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );
            this.model = model;

        }

        @Override
        protected String getI18Nsection()
        {
            return "mailing-lists";
        }

        @Override
        public void renderBody()
        {
            List<MailingList> mailingLists = model.getMailingLists();

            if ( mailingLists == null || mailingLists.isEmpty() )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "nolist" ) );

                endSection();

                return;
            }

            startSection( getTitle() );

            paragraph( getI18nString( "intro" ) );

            startTable();

            // To beautify the display with other archives
            boolean otherArchives = false;
            for ( MailingList m : mailingLists )
            {
                if ( m.getOtherArchives() != null && !m.getOtherArchives().isEmpty() )
                {
                    otherArchives = true;
                }
            }

            String name = getI18nString( "column.name" );
            String subscribe = getI18nString( "column.subscribe" );
            String unsubscribe = getI18nString( "column.unsubscribe" );
            String post = getI18nString( "column.post" );
            String archive = getI18nString( "column.archive" );
            String archivesOther = getI18nString( "column.otherArchives" );

            if ( otherArchives )
            {
                tableHeader( new String[] { name, subscribe, unsubscribe, post, archive, archivesOther } );
            }
            else
            {
                tableHeader( new String[] { name, subscribe, unsubscribe, post, archive } );
            }

            for ( MailingList mailingList : model.getMailingLists() )
            {
                List<String> textRow = new ArrayList<>();

                // Validate here subsribe/unsubsribe lists and archives?
                textRow.add( mailingList.getName() );

                textRow.add( createLinkPatternedText( subscribe, mailingList.getSubscribe() ) );

                textRow.add( createLinkPatternedText( unsubscribe, mailingList.getUnsubscribe() ) );

                if ( mailingList.getPost() != null && mailingList.getPost().length() > 0 )
                {
                    textRow.add( createLinkPatternedText( post, mailingList.getPost() ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( mailingList.getArchive() != null && mailingList.getArchive().length() > 0 )
                {
                    textRow.add( createLinkPatternedText( getArchiveServer( mailingList.getArchive() ),
                                                          mailingList.getArchive() ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( mailingList.getOtherArchives() != null && !mailingList.getOtherArchives().isEmpty() )
                {
                    // For the first line
                    Iterator<String> it = mailingList.getOtherArchives().iterator();
                    String otherArchive = it.next();

                    textRow.add( createLinkPatternedText( getArchiveServer( otherArchive ), otherArchive ) );

                    tableRow( textRow.toArray( new String[textRow.size()] ) );

                    // Other lines...
                    while ( it.hasNext() )
                    {
                        otherArchive = it.next();

                        // Reinit the list to beautify the display
                        textRow = new ArrayList<>();

                        // Name
                        textRow.add( " " );

                        // Subscribe
                        textRow.add( " " );

                        // UnSubscribe
                        textRow.add( " " );

                        // Post
                        textRow.add( " " );

                        // Archive
                        textRow.add( " " );

                        textRow.add( createLinkPatternedText( getArchiveServer( otherArchive ), otherArchive ) );

                        tableRow( textRow.toArray( new String[textRow.size()] ) );
                    }
                }
                else
                {
                    if ( otherArchives )
                    {
                        textRow.add( null );
                    }

                    tableRow( textRow.toArray( new String[textRow.size()] ) );
                }
            }

            endTable();

            endSection();
        }

        /**
         * Convenience method to return the name of a web-based mailing list archive server. <br>
         * For instance, if the archive uri is <code>http://www.mail-archive.com/dev@maven.apache.org</code>, this
         * method return <code>www.mail-archive.com</code>
         *
         * @param uri
         * @return the server name of a web-based mailing list archive server
         */
        private static String getArchiveServer( String uri )
        {
            if ( StringUtils.isEmpty( uri ) )
            {
                return "???UNKNOWN???";
            }

            int at = uri.indexOf( "//" );
            int fromIndex;
            if ( at >= 0 )
            {
                fromIndex = uri.lastIndexOf( '/', at - 1 ) >= 0 ? 0 : at + 2;
            }
            else
            {
                fromIndex = 0;
            }

            int from = uri.indexOf( '/', fromIndex );

            if ( from == -1 )
            {
                return uri.substring( at + 2 );
            }

            return uri.substring( at + 2, from );
        }
    }
}
