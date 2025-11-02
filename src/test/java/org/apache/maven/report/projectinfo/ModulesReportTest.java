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
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.report.projectinfo.stubs.SubProject1Stub;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author ltheussl
 * @version $Id$
 */
public class ModulesReportTest extends AbstractProjectInfoTestCase {
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    @Override
    protected AbstractProjectInfoReport createReportMojo(String goal, File pluginXmlFile) throws Exception {
        AbstractProjectInfoReport mojo = super.createReportMojo(goal, pluginXmlFile);

        mojo.setLog(new SilentLog());

        return mojo;
    }

    /**
     * Test report
     *
     * @throws Exception if any
     */
    @Test
    public void testReport() throws Exception {
        generateReport(getGoal(), "modules-plugin-config.xml");
        assertTrue(getGeneratedReport("modules.html").exists(), "Test html generated");

        URL reportURL = getGeneratedReport("modules.html").toURI().toURL();
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
     * @throws Exception if any
     */
    @Test
    public void testReportModuleLinksVariableSettingsInterpolated() throws Exception {
        String pluginXml = "modules-variable-settings-interpolated-plugin-config.xml";
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/" + pluginXml);
        AbstractProjectInfoReport mojo = createReportMojo(getGoal(), pluginXmlFile);

        class SubProjectStub extends SubProject1Stub {
            @Override
            public File getBasedir() {
                return new File("src/test/resources/plugin-configs/subproject-site-url").getAbsoluteFile();
            }

            @Override
            protected String getPOM() {
                return "pom.xml";
            }
        }
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses("reactorProjects", mojo.getClass());
        field.setAccessible(true);
        field.set(mojo, Collections.singletonList(new SubProjectStub()));

        generateReport(mojo, pluginXmlFile);

        assertFalse(
                new String(Files.readAllBytes(getGeneratedReport("modules.html").toPath()), StandardCharsets.UTF_8)
                        .contains("sitePublishLocation"),
                "Variable 'sitePublishLocation' should be interpolated");
    }

    @Override
    protected String getGoal() {
        return "modules";
    }
}
