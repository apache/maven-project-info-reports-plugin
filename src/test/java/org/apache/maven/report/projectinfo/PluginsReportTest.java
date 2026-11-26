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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Checks the plugin versions listed by the plugins report.
 */
@MojoTest(realRepositorySession = true)
@Basedir("/plugin-configs")
class PluginsReportTest extends AbstractProjectInfoTest {

    @Inject
    private MavenSession mavenSession;

    /**
     * Echoes the requested coordinates back as the plugin's own project, so the version the report renders is the
     * one it resolved for the plugin.
     */
    @Provides
    @SuppressWarnings("unused")
    private ProjectBuilder projectBuilderProvider() throws ProjectBuildingException {
        ProjectBuilder builder = mock(ProjectBuilder.class);
        when(builder.build(isA(Artifact.class), isA(ProjectBuildingRequest.class)))
                .thenAnswer((Answer<ProjectBuildingResult>)
                        invocation -> createProjectBuildingResult(invocation.getArgument(0)));
        return builder;
    }

    @BeforeEach
    void setup() {
        DefaultProjectBuildingRequest pbr = spy(new DefaultProjectBuildingRequest());
        doAnswer(__ -> mavenSession.getRepositorySession()).when(pbr).getRepositorySession();
        when(mavenSession.getProjectBuildingRequest()).thenReturn(pbr);
    }

    /**
     * MPIR-300: a report plugin without a version takes it from build/plugins, then from build/pluginManagement; a
     * version in the reporting section wins over both.
     */
    @Test
    @InjectMojo(goal = "plugins", pom = "plugins-plugin-config.xml")
    void reportPluginVersionComesFromReportingThenPluginsThenPluginManagement(PluginsReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "plugins-plugin-config.xml");
        mojo.execute();

        String html = new String(
                Files.readAllBytes(getTestFile("target/plugins/plugins.html").toPath()), StandardCharsets.UTF_8);

        int reportsStart = html.indexOf(getString("report.plugins.report.title"));
        assertTrue(reportsStart > html.indexOf(getString("report.plugins.build.title")), "report section comes last");
        String reports = html.substring(reportsStart);

        // pluginManagement only
        assertRow(reports, "maven-invoker-plugin", "1.invoker-pluginManagement");
        // build/plugins overrides pluginManagement
        assertRow(reports, "maven-javadoc-plugin", "2.javadoc-plugin");
        // build/plugins only
        assertRow(reports, "maven-checkstyle-plugin", "2.checkstyle-plugin");
        // reporting overrides build/plugins and pluginManagement
        assertRow(reports, "maven-changes-plugin", "3.changes-reporting");
        // defined nowhere
        assertRow(reports, "maven-jxr-plugin", Artifact.RELEASE_VERSION);

        assertFalse(reports.contains("1.javadoc-pluginManagement"));
        assertFalse(reports.contains("2.changes-plugin"));
    }

    private static void assertRow(String html, String artifactId, String version) {
        String row = "<td>" + artifactId + "</td>";
        int start = html.indexOf(row);
        assertTrue(start >= 0, "no row for " + artifactId);
        int end = html.indexOf("</tr>", start);
        assertTrue(
                html.substring(start, end).contains("<td>" + version + "</td>"),
                artifactId + " should be listed with version " + version + " but the row is "
                        + html.substring(start, end));
    }

    private static ProjectBuildingResult createProjectBuildingResult(Artifact artifact) {
        ProjectBuildingResult result = mock(ProjectBuildingResult.class);
        MavenProjectStub stub = new MavenProjectStub();
        stub.setGroupId(artifact.getGroupId());
        stub.setArtifactId(artifact.getArtifactId());
        stub.setVersion(artifact.getVersion());

        when(result.getProject()).thenReturn(stub);
        return result;
    }
}
