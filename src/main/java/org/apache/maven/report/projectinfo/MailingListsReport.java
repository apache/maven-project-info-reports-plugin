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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.net.URI;
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
            new MailingListsRenderer( getLog(), getSink(), getProject().getModel(), getI18N( locale ), locale );

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

        private final Log log;
        private final Model model;

        MailingListsRenderer( Log log, Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );
            this.model = model;
            this.log = log;
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

                if ( StringUtils.isNotEmpty( mailingList.getName() ) )
                {
                    textRow.add( mailingList.getName() );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( StringUtils.isNotEmpty( mailingList.getSubscribe() ) )
                {
                    textRow.add( createURILinkPatternedText( subscribe, mailingList.getSubscribe(), null ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( StringUtils.isNotEmpty( mailingList.getUnsubscribe() ) )
                {
                    textRow.add( createURILinkPatternedText( unsubscribe, mailingList.getUnsubscribe(), null ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( StringUtils.isNotEmpty( mailingList.getPost() ) )
                {
                    textRow.add( createURILinkPatternedText( post, mailingList.getPost(), null ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( mailingList.getArchive() != null && !mailingList.getArchive().isEmpty() )
                {
                    textRow.add( createLinkPatternedText(
                            ProjectInfoReportUtils.getArchiveServer( mailingList.getArchive() ),
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

                    textRow.add( createLinkPatternedText(
                            ProjectInfoReportUtils.getArchiveServer( otherArchive ), otherArchive ) );

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

                        textRow.add( createLinkPatternedText(
                                ProjectInfoReportUtils.getArchiveServer( otherArchive ), otherArchive ) );

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
         * Create a URI link pattern text for a mailing list. If no scheme is provided {@code mailto:}
         * will be prepended by default. If href is null, then <code>defaultHref</code> is used instead.
         *
         * @param text a text.
         * @param href the potential URI to use.
         * @param defaultHref the String to use in case href is null.
         * @return a link pattern.
         * @see #createLinkPatternedText(String,String)
         */
        private String createURILinkPatternedText( String text, String href, String defaultHref )
        {
            if ( href == null || href.isEmpty() )
            {
                return createLinkPatternedText( text, defaultHref );
            }

            try
            {
                URI hrefUri = URI.create( href );
                if ( StringUtils.isNotEmpty( hrefUri.getScheme() ) )
                {
                    return createLinkPatternedText( text, href );
                }
                else
                {
                    return createLinkPatternedText( text, "mailto:" + href );
                }
            }
            catch ( IllegalArgumentException e )
            {
                log.warn( "Invalid mailing list link provided '" + href + "': " + e.getMessage() );
                return href;
            }
        }
    }
}
