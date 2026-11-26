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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks which coordinate snippets the dependency-info report offers for each packaging (MPIR-460).
 */
@MojoTest(realRepositorySession = true)
@Basedir("/plugin-configs")
class DependencyInformationReportTest extends AbstractProjectInfoTest {

    private static final String[] DEPENDENCY_TOOLS = {
        "Apache Ivy", "Groovy Grape", "Gradle/Grails", "Scala SBT", "Leiningen"
    };

    @Test
    @InjectMojo(goal = "dependency-info", pom = "dependency-info-jar-plugin-config.xml")
    void jarPackagingOffersDependencySnippetsForEveryTool(DependencyInformationReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "dependency-info-jar-plugin-config.xml");
        mojo.execute();

        String report = read("target/dependency-info-jar/dependency-info.html");

        assertFalse(report.contains("<pre>&lt;plugin&gt;"));
        assertFalse(report.contains("&lt;/plugin&gt;</pre>"));

        assertTrue(report.contains("<pre>&lt;dependency&gt;"));
        assertTrue(report.contains("&lt;/dependency&gt;</pre>"));
        assertTrue(report.contains("&lt;artifactId&gt;dependency-info-jar&lt;/artifactId&gt;"));
        for (String tool : DEPENDENCY_TOOLS) {
            assertTrue(report.contains(tool), tool);
        }
    }

    @Test
    @InjectMojo(goal = "dependency-info", pom = "dependency-info-maven-plugin-plugin-config.xml")
    void mavenPluginPackagingOffersOnlyThePluginSnippet(DependencyInformationReport mojo) throws Exception {
        readMavenProjectModel(mavenProject, "dependency-info-maven-plugin-plugin-config.xml");
        mojo.execute();

        String report = read("target/dependency-info-maven-plugin/dependency-info.html");

        assertTrue(report.contains("<pre>&lt;plugin&gt;"));
        assertTrue(report.contains("&lt;/plugin&gt;</pre>"));
        assertTrue(report.contains("&lt;artifactId&gt;dependency-info-maven-plugin&lt;/artifactId&gt;"));

        assertFalse(report.contains("<pre>&lt;dependency&gt;"));
        assertFalse(report.contains("&lt;/dependency&gt;</pre>"));
        for (String tool : DEPENDENCY_TOOLS) {
            assertFalse(report.contains(tool), tool);
        }
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(getTestFile(path).toPath()), StandardCharsets.UTF_8);
    }
}
