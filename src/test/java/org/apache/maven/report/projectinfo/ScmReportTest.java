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
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@MojoTest(realRepositorySession = true)
@Basedir("/plugin-configs")
class ScmReportTest extends AbstractProjectInfoTest {
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
    @InjectMojo(goal = "scm", pom = "scm-plugin-config.xml")
    void testReport(ScmReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "scm-plugin-config.xml");

        mojo.execute();

        URL reportURL = getTestFile("target/scm/scm.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("scm project info", getString("report.scm.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        // Last one is footer noise
        assertEquals(8, textBlocks.length - 1);
        assertEquals(getString("report.scm.overview.title"), textBlocks[1].getText());
        assertEquals(getString("report.scm.general.intro"), textBlocks[2].getText());
        assertEquals(getString("report.scm.webaccess.title"), textBlocks[3].getText());
        assertEquals(getString("report.scm.webaccess.nourl"), textBlocks[4].getText());
        assertEquals(getString("report.scm.accessbehindfirewall.title"), textBlocks[5].getText());
        assertEquals(getString("report.scm.accessbehindfirewall.general.intro"), textBlocks[6].getText());
    }

    /**
     * Test report with wrong URL
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "scm", pom = "scm-plugin-config.xml")
    @MojoParameter(name = "anonymousConnection", value = "scm:svn")
    void testReportWithWrongUrl1(ScmReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "scm-plugin-config.xml");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("This SCM url 'scm:svn' is invalid"));
    }

    @Test
    @InjectMojo(goal = "scm", pom = "scm-plugin-config.xml")
    @MojoParameter(name = "anonymousConnection", value = "scm:svn:http")
    void testReportWithWrongUrl2(ScmReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "scm-plugin-config.xml");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("This SCM url 'scm:svn:http' is invalid"));
    }

    @Test
    @InjectMojo(goal = "scm", pom = "scm-plugin-config.xml")
    @MojoParameter(name = "anonymousConnection", value = "scm")
    void testReportWithWrongUrl3(ScmReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "scm-plugin-config.xml");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("This SCM url 'scm' is invalid"));
    }
}
