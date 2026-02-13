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
import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@MojoTest(realRepositorySession = true)
public class TeamReportTest extends AbstractProjectInfoTest {
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
    @InjectMojo(goal = "team", pom = "team-plugin-config.xml")
    @Basedir("/plugin-configs")
    public void testReport(TeamReport mojo) throws Exception {
        //        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/" +
        // "team-plugin-config.xml");
        //        AbstractProjectInfoReport mojo = createReportMojo(getGoal(), pluginXmlFile);
        setVariableValueToObject(mojo, "showAvatarImages", Boolean.TRUE);
        setVariableValueToObject(mojo, "externalAvatarImages", Boolean.TRUE);
        setVariableValueToObject(mojo, "avatarProviderName", "gravatar");
        setVariableValueToObject(mojo, "avatarBaseUrl", "https://www.gravatar.com/avatar/");

        readMavenProjectModel(mavenProject, "team-plugin-config.xml");

        mojo.execute();

        URL reportURL = getTestFile("target/team/team.html").toURI().toURL();
        assertNotNull(reportURL);

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest(reportURL.toString());
        WebResponse response = WEB_CONVERSATION.getResponse(request);

        // Basic HTML tests
        assertTrue(response.isHTML());
        assertTrue(response.getContentLength() > 0);

        // Test the Page title
        String expectedTitle = prepareTitle("team project info", getString("report.team.title"));
        assertEquals(expectedTitle, response.getTitle());

        assertTrue(response.getText().contains("gravatar"));

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        // Last one is footer noise
        assertEquals(9, textBlocks.length - 1);
        assertEquals(getString("report.team.intro.title"), textBlocks[1].getText());
        assertEquals(getString("report.team.intro.description1"), textBlocks[2].getText());
        assertEquals(getString("report.team.intro.description2"), textBlocks[3].getText());
        assertEquals(getString("report.team.developers.title"), textBlocks[4].getText());
        assertEquals(getString("report.team.developers.intro"), textBlocks[5].getText());
        assertEquals(getString("report.team.contributors.title"), textBlocks[6].getText());
        assertEquals(getString("report.team.nocontributor"), textBlocks[7].getText());

        WebTable[] tables = response.getTables();
        assertEquals(1, tables.length);
        TableCell emailCell = tables[0].getTableCell(1, 3);
        assertEquals("vsiveton@apache.org", emailCell.getText());
        WebLink[] links = emailCell.getLinks();
        assertEquals(1, links.length);
        assertEquals("mailto:vsiveton@apache.org", links[0].getURLString());
    }
}
