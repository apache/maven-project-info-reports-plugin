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
package org.apache.maven.report.projectinfo;

import java.io.File;
import java.net.URL;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@MojoTest(realRepositorySession = true)
@Basedir("/plugin-configs")
public class MailingListsReportTest extends AbstractProjectInfoTest {
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    /**
     * Test report
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "mailing-lists", pom = "mailing-lists-plugin-config.xml")
    public void testReport(MailingListsReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "mailing-lists-plugin-config.xml");
        mojo.execute();

        URL reportURL =
                getTestFile("target/mailing-lists/mailing-lists.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("mailing lists project info", getString("report.mailing-lists.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.mailing-lists.title"), textBlocks[1].getText());
        assertEquals(getString("report.mailing-lists.intro"), textBlocks[2].getText());

        // MPIR-385 + MPIR-401: Test links are URIs otherwise assume a plain email address
        String post = getString("report.mailing-lists.column.post");
        WebLink[] postLinks = response.getMatchingLinks(WebLink.MATCH_CONTAINED_TEXT, post);
        assertEquals("mailto:test@maven.apache.org", postLinks[0].getAttribute("href"));
        assertEquals("mailto:test2@maven.apache.org", postLinks[1].getAttribute("href"));
        String subscribe = getString("report.mailing-lists.column.subscribe");
        WebLink[] subscribeLinks = response.getMatchingLinks(WebLink.MATCH_CONTAINED_TEXT, subscribe);
        assertEquals("MAILTO:test-subscribe@maven.apache.org", subscribeLinks[0].getAttribute("href"));
        assertEquals("MAILTO:test-subscribe2@maven.apache.org", subscribeLinks[1].getAttribute("href"));
        String unsubscribe = getString("report.mailing-lists.column.unsubscribe");
        WebLink[] unsubscribeLinks = response.getMatchingLinks(WebLink.MATCH_CONTAINED_TEXT, unsubscribe);
        assertEquals(1, unsubscribeLinks.length);
        assertEquals("https://example.com/unsubscribe", unsubscribeLinks[0].getAttribute("href"));
    }

    /**
     * Test custom bundle
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "mailing-lists", pom = "custom-bundle/plugin-config.xml")
    public void testCustomBundle(MailingListsReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "custom-bundle/plugin-config.xml");
        mojo.execute();

        URL reportURL = getTestFile("target/mailing-lists-custom-bundle/mailing-lists.html")
                .toURI()
                .toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.mailing-lists.title"), textBlocks[1].getText());
        assertEquals("mail list intro text foo", textBlocks[2].getText());
    }

    /**
     * Test report in French (MPIR-59)
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "mailing-lists", pom = "mailing-lists-plugin-config-fr.xml")
    public void testFrenchReport(MailingListsReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "mailing-lists-plugin-config-fr.xml");
        mojo.execute();

        File reportFile = getTestFile("target/mailing-lists-fr/mailing-lists.html");
        assertTrue(reportFile.exists());
    }

    /**
     * Test invalid links (MPIR-404)
     * Those should only lead to a WARN but not an exception
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "mailing-lists", pom = "mailing-lists-plugin-config-invalidlink.xml")
    public void testInvalidLink(MailingListsReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "mailing-lists-plugin-config-invalidlink.xml");
        mojo.execute();

        File reportFile = getTestFile("target/mailing-lists-invalidlink/mailing-lists.html");
        assertTrue(reportFile.exists());
    }
}
