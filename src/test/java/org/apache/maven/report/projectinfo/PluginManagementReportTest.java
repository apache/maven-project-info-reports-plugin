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
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nick Stolwijk
 * @since 2.1
 */
public class PluginManagementReportTest extends AbstractProjectInfoTestCase {
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    @Override
    protected AbstractProjectInfoReport createReportMojo(String goal, File pluginXmlFile) throws Exception {
        AbstractProjectInfoReport mojo = super.createReportMojo(goal, pluginXmlFile);

        ProjectBuilder builder = mock(ProjectBuilder.class);

        when(builder.build(isA(Artifact.class), isA(ProjectBuildingRequest.class)))
                .thenAnswer(new Answer<ProjectBuildingResult>() {
                    @Override
                    public ProjectBuildingResult answer(InvocationOnMock invocation) throws Throwable {
                        return createProjectBuildingResult(invocation.getArgument(0), "http://m.a.o/");
                    }
                });

        setVariableValueToObject(mojo, "projectBuilder", builder);

        return mojo;
    }

    /**
     * Test report
     *
     * @throws Exception if any
     */
    @Test
    public void testReport() throws Exception {
        generateReport(getGoal(), "plugin-management-plugin-config.xml");
        org.junit.jupiter.api.Assertions.assertTrue(
                getGeneratedReport("plugin-management.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("plugin-management.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle =
                prepareTitle("plugin management project info", getString("report.plugin-management.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the tables
        WebTable[] webTables = response.getTables();
        assertEquals(1, webTables.length);

        assertEquals(3, webTables[0].getColumnCount());
        assertEquals(3, webTables[0].getRowCount());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.plugin-management.title"), textBlocks[1].getText());
    }

    /**
     * Test report with excludes (to solve MPIR-375)
     *
     * @throws Exception if any
     */
    @Test
    public void testReportEclipseM2EPluginLifecycleMapping() throws Exception {
        generateReport(getGoal(), "plugin-management-plugin-config-MPIR-375.xml");
        org.junit.jupiter.api.Assertions.assertTrue(
                getGeneratedReport("plugin-management.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("plugin-management.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle =
                prepareTitle("plugin management project info", getString("report.plugin-management.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the tables
        WebTable[] webTables = response.getTables();
        assertEquals(1, webTables.length);

        // generated table for the plugin management
        assertEquals(3, webTables[0].getColumnCount());
        assertEquals(2, webTables[0].getRowCount());
        // row 0 are the table titles
        // row 1 is the m-javadoc-plugin
        assertEquals("org.apache.maven.plugins", webTables[0].getCellAsText(1, 0));
        assertEquals("maven-javadoc-plugin", webTables[0].getCellAsText(1, 1));
        assertEquals("3.0.1", webTables[0].getCellAsText(1, 2));

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.plugin-management.title"), textBlocks[1].getText());
    }

    private static ProjectBuildingResult createProjectBuildingResult(Artifact artifact, String url) {
        ProjectBuildingResult result = mock(ProjectBuildingResult.class);
        MavenProjectStub stub = new MavenProjectStub();
        stub.setGroupId(artifact.getGroupId());
        stub.setArtifactId(artifact.getArtifactId());
        stub.setVersion(artifact.getVersion());
        stub.setUrl(url);

        when(result.getProject()).thenReturn(stub);

        return result;
    }

    @Override
    protected String getGoal() {
        return "plugin-management";
    }
}
