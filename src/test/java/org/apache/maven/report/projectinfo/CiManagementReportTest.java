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
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.junit.jupiter.api.Test;
import org.w3c.dom.html.HTMLAnchorElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class CiManagementReportTest extends AbstractProjectInfoTestCase {
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
        generateReport(getGoal(), "ci-management-plugin-config.xml");
        assertTrue(
                getGeneratedReport("ci-management.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("ci-management.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("ci mangement project info", getString("report.ci-management.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.ci-management.name"), textBlocks[1].getText());
        assertEquals(getString("report.ci-management.nocim"), textBlocks[2].getText());
    }

    /**
     * When a ciManagement section is present, test that the correct name and link text are chosen
     * from the 'system' property.
     *
     * @throws Exception if any
     */
    @Test
    public void testCiNameReport() throws Exception {
        generateReport(getGoal(), "ci-management-plugin-with-ci-section-config.xml");
        assertTrue(
                getGeneratedReport("ci-management.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("ci-management.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        TextBlock textBlock = textBlocks[2];
        assertTrue(textBlock.getText().startsWith("This project uses "));
        assertEquals(3, textBlock.getNode().getChildNodes().getLength());
        HTMLAnchorElement anchor =
                (HTMLAnchorElement) textBlock.getNode().getChildNodes().item(1);
        assertEquals("https://www.jetbrains.com/teamcity/", anchor.getAttribute("href"));
        assertEquals("TeamCity", anchor.getFirstChild().getNodeValue());
    }

    @Override
    protected String getGoal() {
        return "ci-management";
    }
}
