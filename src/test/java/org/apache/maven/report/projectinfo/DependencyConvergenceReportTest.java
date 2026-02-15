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

import java.net.URL;
import java.util.Arrays;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@MojoTest(realRepositorySession = true)
@Basedir("/plugin-configs")
public class DependencyConvergenceReportTest extends AbstractProjectInfoTest {

    @Inject
    private MavenSession mavenSession;

    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    @BeforeEach
    void setup() throws Exception {
        DefaultProjectBuildingRequest pbr = spy(new DefaultProjectBuildingRequest());
        doAnswer(__ -> mavenSession.getRepositorySession()).when(pbr).getRepositorySession();
        when(mavenSession.getProjectBuildingRequest()).thenReturn(pbr);

        MavenProject sub1 = new MavenProject();
        readMavenProjectModel(sub1, "subproject1/pom.xml");
        setArtifactForProject(sub1);
        addDependencyToProject(sub1);

        MavenProject sub2 = new MavenProject();
        readMavenProjectModel(sub1, "subproject2/pom.xml");
        setArtifactForProject(sub2);
        addDependencyToProject(sub2);

        readMavenProjectModel(mavenProject, "dependency-convergence-plugin-config.xml");
        setArtifactForProject(mavenProject);

        when(mavenSession.getProjects()).thenReturn(Arrays.asList(mavenProject, sub1, sub2));
    }

    private void addDependencyToProject(MavenProject project) {
        Dependency d = new Dependency();
        d.setGroupId("org.junit.jupiter");
        d.setArtifactId("junit-jupiter-api");
        d.setVersion("5.14.2");
        d.setScope(Artifact.SCOPE_COMPILE);
        project.getDependencies().add(d);
    }

    /**
     * Test report
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "dependency-convergence", pom = "dependency-convergence-plugin-config.xml")
    public void testReport(DependencyConvergenceReport mojo) throws Exception {
        mojo.execute();
        URL reportURL = getTestFile("target/dependency-convergence/dependency-convergence.html")
                .toURI()
                .toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle(
                "dependency convergence project info", getString("report.dependency-convergence.reactor.title"));
        assertEquals(expectedTitle, response.getTitle());

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals(getString("report.dependency-convergence.reactor.name"), textBlocks[1].getText());
    }
}
