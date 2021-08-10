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

import java.net.URL;
import java.util.Locale;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class MailingListsReportTest
    extends AbstractProjectInfoTestCase
{
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    /**
     * Test report
     *
     * @throws Exception if any
     */
    public void testReport()
        throws Exception
    {
        generateReport( "mailing-lists", "mailing-lists-plugin-config.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "mailing-lists.html" ).exists() );

        URL reportURL = getGeneratedReport( "mailing-lists.html" ).toURI().toURL();
        assertNotNull( reportURL );

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest( reportURL.toString() );
        WebResponse response = WEB_CONVERSATION.getResponse( request );

        // Basic HTML tests
        assertTrue( response.isHTML() );
        assertTrue( response.getContentLength() > 0 );

        // Test the Page title
        String expectedTitle = prepareTitle( getString( "report.mailing-lists.name" ),
            getString( "report.mailing-lists.title" ) );
        assertEquals( expectedTitle, response.getTitle() );

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals( getString( "report.mailing-lists.title" ), textBlocks[0].getText() );
        assertEquals( getString( "report.mailing-lists.intro" ), textBlocks[1].getText() );

        // MPIR-385 + MPIR-401: Test links are URIs otherwise assume a plain email address
        String post = getString("report.mailing-lists.column.post");
        WebLink[] postLinks = response.getMatchingLinks( WebLink.MATCH_CONTAINED_TEXT, post );
        assertEquals( "mailto:test@maven.apache.org", postLinks[0].getAttribute( "href" ) );
        assertEquals( "mailto:test2@maven.apache.org", postLinks[1].getAttribute( "href" ) );
        String subscribe = getString("report.mailing-lists.column.subscribe");
        WebLink[] subscribeLinks = response.getMatchingLinks( WebLink.MATCH_CONTAINED_TEXT, subscribe );
        assertEquals( "MAILTO:test-subscribe@maven.apache.org", subscribeLinks[0].getAttribute( "href" ) );
        assertEquals( "MAILTO:test-subscribe2@maven.apache.org", subscribeLinks[1].getAttribute( "href" ) );
        String unsubscribe = getString("report.mailing-lists.column.unsubscribe");
        WebLink[] unsubscribeLinks = response.getMatchingLinks( WebLink.MATCH_CONTAINED_TEXT, unsubscribe );
        assertTrue( unsubscribeLinks.length == 1 );
        assertEquals( "https://example.com/unsubscribe", unsubscribeLinks[0].getAttribute( "href" ) );
    }

    /**
     * Test report in French (MPIR-59)
     *
     * @throws Exception if any
     */
    public void testFrenchReport()
        throws Exception
    {
        Locale oldLocale = Locale.getDefault();

        try
        {
            Locale.setDefault( Locale.FRENCH );

            generateReport( "mailing-lists", "mailing-lists-plugin-config.xml" );
            assertTrue( "Test html generated", getGeneratedReport( "mailing-lists.html" ).exists() );
        }
        finally
        {
            Locale.setDefault( oldLocale );
        }
    }

    /**
     * Test invalid links (MPIR-404)
     * Those should only lead to a WARN but not an exception
     * @throws Exception if any
     */
    public void testInvalidLink()
        throws Exception
    {
        generateReport( "mailing-lists", "mailing-lists-plugin-config-invalidlink.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "mailing-lists.html" ).exists() );
    }
}
