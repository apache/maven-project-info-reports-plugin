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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author ltheussl
 * @version $Id$
 */
@MojoTest(realRepositorySession = true)
@Basedir("/plugin-configs")
class ModulesReportTest extends AbstractProjectInfoTest {

    @Inject
    private MavenSession mavenSession;
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    @BeforeEach
    void setup() {
        DefaultProjectBuildingRequest pbr = spy(new DefaultProjectBuildingRequest());
        doAnswer(__ -> mavenSession.getRepositorySession()).when(pbr).getRepositorySession();
        when(mavenSession.getProjectBuildingRequest()).thenReturn(pbr);
    }

    /**
     * Test report
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "modules", pom = "modules-plugin-config.xml")
    void testReport(ModulesReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "modules-plugin-config.xml");
        mojo.execute();

        URL reportURL = getTestFile("target/modules/modules.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("modules project info", getString("report.modules.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        // Last one is footer noise
        assertEquals(4, textBlocks.length - 1);
        assertEquals(getString("report.modules.title"), textBlocks[1].getText());
        assertEquals(getString("report.modules.intro"), textBlocks[2].getText());

        String[][] cellTexts = response.getTables()[0].asText();
        assertEquals(3, cellTexts.length);
        assertEquals(2, cellTexts[0].length);
        assertEquals(getString("report.modules.header.name"), cellTexts[0][0]);
        assertEquals(getString("report.modules.header.description"), cellTexts[0][1]);
        assertEquals("project1", cellTexts[1][0]);
        assertEquals("-", cellTexts[1][1]);
        assertEquals("project2", cellTexts[2][0]);
        assertEquals("project2 description", cellTexts[2][1]);
    }

    /**
     * Test report with variable from settings interpolation in modules URL links (MPIR-349)
     *
     */
    @Nested
    class ReportModuleLinksVariableSettingsInterpolated {

        @Inject
        private Settings settings;

        @BeforeEach
        void setupProject() throws Exception {
            readMavenProjectModel(mavenProject, "modules-variable-settings-interpolated-plugin-config.xml");

            MavenProject subProject = new MavenProject();
            readMavenProjectModel(subProject, "subproject-site-url/pom.xml");
            subProject.setFile(getTestFile("subproject-site-url/pom.xml"));

            when(mavenSession.getProjects()).thenReturn(Arrays.asList(mavenProject, subProject));

            Profile p = new Profile();
            p.setId("site-location");
            p.addProperty("sitePublishLocation", "file://tmp/sitePublish");
            settings.addProfile(p);
            // should have impact on the test ...
            //            settings.setActiveProfiles(Collections.singletonList("site-location"));
        }

        @Test
        @InjectMojo(goal = "modules", pom = "modules-variable-settings-interpolated-plugin-config.xml")
        void testReportModuleLinksVariableSettingsInterpolated(ModulesReport mojo) throws Exception {
            mojo.execute();

            File reportFile = getTestFile("target/modules-variable/modules.html");
            // TODO check this assertions, even if without profiles it is ok
            assertFalse(
                    new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8)
                            .contains("sitePublishLocation"),
                    "Variable 'sitePublishLocation' should be interpolated");
        }
    }
}
