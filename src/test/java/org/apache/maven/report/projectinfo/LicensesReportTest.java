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

import javax.inject.Inject;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.SessionData;
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
class LicensesReportTest extends AbstractProjectInfoTest {
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    @Inject
    private MavenSession mavenSession;

    /**
     * Test report
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "licenses", pom = "licenses-plugin-config.xml")
    void testReport(LicensesReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "licenses-plugin-config.xml");
        mojo.execute();

        URL reportURL = getTestFile("target/licenses/licenses.html").toURI().toURL();
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
    @InjectMojo(goal = "licenses", pom = "licenses-plugin-config-linkonly.xml")
    void testReportLinksOnly(LicensesReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "licenses-plugin-config-linkonly.xml");
        mojo.execute();

        URL reportURL =
                getTestFile("target/licenses-linkonly/licenses.html").toURI().toURL();
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

    /**
     * A later module of the same build must not download a license text the build already has: the report renders
     * whatever the session-scoped cache holds for the URL.
     */
    @Test
    @InjectMojo(goal = "licenses", pom = "licenses-plugin-config.xml")
    void testLicenseContentComesFromSessionCache(LicensesReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "licenses-plugin-config.xml");

        String cachedText = "License text cached earlier in this build (MPIR-586)";
        Map<String, Object> cache = new ConcurrentHashMap<>();
        cache.put(
                LicensesReport.licenseContentCacheKey(new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"), null),
                cachedText);
        mavenSession.getRepositorySession().getData().set(LicensesReport.LICENSE_CONTENT_CACHE_KEY, cache);

        mojo.execute();

        URL reportURL = getTestFile("target/licenses/licenses.html").toURI().toURL();
        WebResponse response = WEB_CONVERSATION.getResponse(new GetMethodWebRequest(reportURL.toString()));
        assertTrue(response.getText().contains(cachedText), "report should render the cached license text");
        assertEquals(1, cache.size(), "no other URL should have been fetched");
    }

    /**
     * The first render of a license URL in a build puts its text into the session-scoped cache, under the key later
     * modules will look up. Uses a local license file so the test needs no network.
     */
    @Test
    @InjectMojo(goal = "licenses", pom = "licenses-plugin-config-local.xml")
    @SuppressWarnings("unchecked")
    void testLicenseContentIsCachedForLaterModules(LicensesReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "licenses-plugin-config-local.xml");
        SessionData sessionData = mavenSession.getRepositorySession().getData();
        sessionData.set(LicensesReport.LICENSE_CONTENT_CACHE_KEY, null);

        mojo.execute();

        Map<String, Object> cache = (Map<String, Object>) sessionData.get(LicensesReport.LICENSE_CONTENT_CACHE_KEY);
        assertNotNull(cache, "the first render should create the session cache");
        URL licenseUrl = new File(mavenProject.getBasedir(), "licenses-local-LICENSE.txt")
                .toURI()
                .toURL();
        String expectedKey = LicensesReport.licenseContentCacheKey(licenseUrl, null);
        assertEquals(1, cache.size(), cache.keySet().toString());
        assertTrue(
                ((String) cache.get(expectedKey)).contains("Local license text for MPIR-586"),
                "cached text should be the license file content");
    }
}
