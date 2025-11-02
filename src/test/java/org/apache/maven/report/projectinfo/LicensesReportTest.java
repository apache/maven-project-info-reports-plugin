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

import java.net.URL;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class LicensesReportTest extends AbstractProjectInfoTestCase {
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
    public void testReport() throws Exception {
        generateReport(getGoal(), "licenses-plugin-config.xml");
        org.junit.jupiter.api.Assertions.assertTrue(
                getGeneratedReport("licenses.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("licenses.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("licenses project info", getString("report.licenses.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.licenses.overview.title"), textBlocks[1].getText());
        assertEquals(getString("report.licenses.overview.intro"), textBlocks[2].getText());
        assertEquals(getString("report.licenses.title"), textBlocks[3].getText());
        assertEquals("The Apache Software License, Version 2.0", textBlocks[4].getText());

        // only 1 link in default report
        final WebLink[] links = response.getLinks();
        assertEquals(2, links.length);
        assertEquals("https://maven.apache.org/", links[1].getURLString());
    }

    @Test
    public void testReportLinksOnly() throws Exception {
        generateReport(getGoal(), "licenses-plugin-config-linkonly.xml");
        org.junit.jupiter.api.Assertions.assertTrue(
                getGeneratedReport("licenses.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("licenses.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("licenses project info", getString("report.licenses.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.licenses.overview.title"), textBlocks[1].getText());
        assertEquals(getString("report.licenses.overview.intro"), textBlocks[2].getText());
        assertEquals(getString("report.licenses.title"), textBlocks[3].getText());
        assertEquals("The Apache Software License, Version 2.0", textBlocks[4].getText());

        // here's our specific test
        final WebLink[] links = response.getLinks();
        assertEquals(3, links.length);
        assertEquals("http://maven.apache.org", links[0].getURLString());
        assertEquals("https://www.apache.org/licenses/LICENSE-2.0.txt", links[1].getURLString());
        assertEquals("https://www.apache.org/licenses/LICENSE-2.0.txt", links[1].getText());
    }

    @Override
    protected String getGoal() {
        return "licenses";
    }
}
