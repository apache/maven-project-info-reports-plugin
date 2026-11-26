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
package org.apache.maven.report.projectinfo.dependencies.renderer;

import javax.inject.Inject;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.doxia.sink.impl.Xhtml5BaseSink;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.DependencyNode;
import org.apache.maven.report.projectinfo.dependencies.DependencyNodes;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.JarFixtures.Entry;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;
import org.codehaus.plexus.i18n.I18N;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Renders the dependencies report from fixture jars and checks the file details table and the license columns. The
 * jars are real zip files with real class files, so the jar analysis runs too; nothing is resolved from a repository.
 */
@MojoTest
class DependenciesRendererTest {

    @Inject
    private I18N i18n;

    @TempDir
    private File tmp;

    private final Map<String, License> licenses = new HashMap<>();

    private final Map<String, String> licenseMappings = new HashMap<>();

    /**
     * MPIR-431: the summary rows of the file details table. A row of totals, then one row per scope that has
     * dependencies with each cell prefixed by the scope, and one Java version cell spanning the compile, runtime and
     * provided rows.
     */
    @Test
    void fileDetailsSummaryHasOneRowPerScopeAndASharedJavaVersionCell() throws Exception {
        Artifact compile = artifact("compile", "compile-dep", jar("compile.jar", 45_500, clazz("p/A", 47)));
        Artifact runtime = artifact("runtime", "runtime-dep", jar("runtime.jar", 284_200, clazz("p/B", 50)));
        Artifact provided = artifact("provided", "provided-dep", jar("provided.jar", 85_700, clazz("p/C", 49)));
        Artifact test1 = artifact("test", "test-dep-1", jar("test1.jar", 1_000_100, clazz("p/D", 52)));
        Artifact test2 = artifact("test", "test-dep-2", jar("test2.jar", 1_199_900, clazz("q/E", 51)));

        String html = render(true, compile, runtime, provided, test1, test2);

        assertRows(
                html,
                row(right("5"), right("2.6 MB"), right("10"), right("5"), right("5"), center("1.8"), right("-")),
                row(
                        right("compile: 1"),
                        right("compile: 45.5 kB"),
                        right("compile: 2"),
                        right("compile: 1"),
                        right("compile: 1"),
                        "<td rowspan=\"3\" style=\"text-align: center; vertical-align: middle;\">1.6</td>",
                        right("-")),
                row(
                        right("runtime: 1"),
                        right("runtime: 284.2 kB"),
                        right("runtime: 2"),
                        right("runtime: 1"),
                        right("runtime: 1"),
                        right("-")),
                row(
                        right("provided: 1"),
                        right("provided: 85.7 kB"),
                        right("provided: 2"),
                        right("provided: 1"),
                        right("provided: 1"),
                        right("-")),
                row(
                        right("test: 2"),
                        right("test: 2.2 MB"),
                        right("test: 4"),
                        right("test: 2"),
                        right("test: 2"),
                        center("1.8"),
                        right("-")));
    }

    /**
     * MPIR-437: a jar without any class has no Java version; the cell, like the debug information cell, shows a dash
     * rather than staying empty. The totals show a dash where there is nothing to count.
     */
    @Test
    void jarWithoutClassesShowsDashesForJavaVersion() throws Exception {
        Artifact resources = artifact(
                "compile",
                "resources-only",
                jar(
                        "resources-only.jar",
                        78_800,
                        Entry.resource("messages_de.properties"),
                        Entry.resource("messages_fr.properties"),
                        Entry.resource("messages_it.properties")));

        String html = render(true, resources);

        // file row: no Java version and no debug information to report
        assertRows(
                html,
                row(
                        left("resources-only.jar"),
                        right("78.8 kB"),
                        right("4"),
                        right("0"),
                        right("0"),
                        center("-"),
                        center("-")));
        // totals row: nothing to total for classes and packages
        assertRows(
                html, row(right("1"), right("78.8 kB"), right("4"), right("-"), right("-"), center("-"), right("-")));
    }

    /**
     * MPIR-455: a multi-release jar is one row for the jar, then one for the root content and one per versioned
     * runtime, each with its own entry and class counts and Java version. A jar that has only a module descriptor in
     * a versioned directory has no classes at the root.
     */
    @Test
    void multiReleaseJarListsRootAndVersionedRuntimes() throws Exception {
        String bullet = "&#160;&#160;&#160;&#x2022; ";
        Artifact moduleInfoOnly = artifact(
                "runtime",
                "module-info-only",
                jar(
                        "module-info-only.jar",
                        3_300,
                        Entry.manifest("Multi-Release: true"),
                        Entry.resource("README.txt"),
                        Entry.moduleInfo("META-INF/versions/11/module-info.class", 55)));
        Artifact multiRelease = artifact(
                "test",
                "multi-release",
                jar(
                        "multi-release.jar",
                        10_200,
                        Entry.manifest("Multi-Release: true"),
                        clazz("p/R", 52),
                        Entry.classFile("META-INF/versions/9/p/R.class", "p/R", 53),
                        Entry.classFile("META-INF/versions/11/p/R.class", "p/R", 55)));

        String html = render(true, moduleInfoOnly, multiRelease);

        assertRows(
                html,
                row(
                        left("module-info-only.jar"),
                        right("3.3 kB"),
                        right("4"),
                        right("-"),
                        right("-"),
                        center("-"),
                        center("-")),
                row(left(bullet + "Root"), right("-"), right("3"), right("0"), right("0"), center("-"), center("-")),
                row(
                        left(bullet + "Versioned"),
                        right("-"),
                        right("1"),
                        right("1"),
                        right("1"),
                        center("11"),
                        center("No")),
                row(
                        left("multi-release.jar"),
                        right("10.2 kB"),
                        right("5"),
                        right("-"),
                        right("-"),
                        center("-"),
                        center("-")),
                row(left(bullet + "Root"), right("-"), right("3"), right("1"), right("1"), center("1.8"), center("No")),
                row(
                        left(bullet + "Versioned"),
                        right("-"),
                        right("1"),
                        right("1"),
                        right("1"),
                        center("9"),
                        center("No")),
                row(
                        left(bullet + "Versioned"),
                        right("-"),
                        right("1"),
                        right("1"),
                        right("1"),
                        center("11"),
                        center("No")));
    }

    /** The Java version column is the highest class file version of the jar: 1.7 for major 51, 1.8 for 52. */
    @Test
    void javaVersionIsReadFromTheClassFiles() throws Exception {
        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(48, "1.4");
        expected.put(49, "1.5");
        expected.put(50, "1.6");
        expected.put(51, "1.7");
        expected.put(52, "1.8");
        expected.put(53, "9");
        expected.put(55, "11");

        List<Artifact> artifacts = new ArrayList<>();
        for (int major : expected.keySet()) {
            artifacts.add(artifact(
                    "compile", "dep" + major, jar("dep" + major + ".jar", 2_000, clazz("p/C" + major, major))));
        }

        String html = render(true, artifacts.toArray(new Artifact[0]));

        for (Map.Entry<Integer, String> e : expected.entrySet()) {
            assertRows(
                    html,
                    row(
                            left("dep" + e.getKey() + ".jar"),
                            right("2 kB"),
                            right("2"),
                            right("1"),
                            right("1"),
                            center(e.getValue()),
                            center("No")));
        }
    }

    /**
     * The debug information cell says "Yes" for a jar whose classes have line numbers, "No" for one without, and the
     * summary counts the jars that have it, per scope.
     */
    @Test
    void debugInformationIsReportedPerJarAndCountedInTheSummary() throws Exception {
        Artifact withDebug =
                artifact("compile", "with-debug", jar("with-debug.jar", 2_000, debugClass("p/WithDebug", 52)));
        Artifact withoutDebug =
                artifact("compile", "without-debug", jar("without-debug.jar", 2_000, clazz("p/Plain", 52)));

        String html = render(true, withDebug, withoutDebug);

        assertRows(
                html,
                row(
                        left("with-debug.jar"),
                        right("2 kB"),
                        right("2"),
                        right("1"),
                        right("1"),
                        center("1.8"),
                        center("Yes")),
                row(
                        left("without-debug.jar"),
                        right("2 kB"),
                        right("2"),
                        right("1"),
                        right("1"),
                        center("1.8"),
                        center("No")));
        assertRows(
                html,
                row(right("2"), right("4 kB"), right("4"), right("2"), right("2"), center("1.8"), right("1")),
                row(
                        right("compile: 2"),
                        right("compile: 4 kB"),
                        right("compile: 4"),
                        right("compile: 2"),
                        right("compile: 2"),
                        center("1.8"),
                        right("compile: 1")));
    }

    /**
     * MPIR-407: a license name that has a mapping is shown under the mapped name in the dependency tables, the
     * dependency details and the license listing; one without a mapping is left alone.
     */
    @Test
    void licenseMappingsRenameLicensesEverywhere() throws Exception {
        licenseMappings.put("The Apache Software License, Version 2.0", "Apache License, Version 2.0");
        licenseMappings.put("Apache Public License 2.0", "Apache License, Version 2.0");
        licenses.put("old-name", license("The Apache Software License, Version 2.0"));
        licenses.put("other-old-name", license("Apache Public License 2.0"));
        licenses.put("unmapped", license("MIT License"));

        String html = render(
                false,
                artifact("compile", "old-name", null),
                artifact("compile", "other-old-name", null),
                artifact("compile", "unmapped", null));

        assertFalse(html.contains("The Apache Software License, Version 2.0"), "mapped name is gone");
        assertFalse(html.contains("Apache Public License 2.0"), "mapped name is gone");
        assertTrue(html.contains(">Apache License, Version 2.0</a>"), "dependency table and details use the new name");
        assertTrue(html.contains(">MIT License</a>"), "an unmapped license is kept");
        // the listing groups the three artifacts by license: both mapped ones under one name
        assertTrue(
                Pattern.compile("Apache License, Version 2.0: </b>other-old-name, old-name|"
                                + "Apache License, Version 2.0: </b>old-name, other-old-name")
                        .matcher(html.replaceAll("\\s+", " "))
                        .find(),
                "both artifacts are listed under the mapped license");
    }

    private static License license(String name) {
        License license = new License();
        license.setName(name);
        license.setUrl("https://example.org/" + name.replace(' ', '-'));
        return license;
    }

    private File jar(String name, long length, Entry... entries) throws Exception {
        return JarFixtures.jar(tmp, name, length, entries);
    }

    private Artifact artifact(String scope, String artifactId, File file) {
        Artifact artifact = new DefaultArtifact(
                "org.example", artifactId, "1.0", scope, "jar", null, new DefaultArtifactHandler("jar"));
        artifact.setFile(file);
        return artifact;
    }

    private static Entry debugClass(String internalName, int major) throws Exception {
        return Entry.classFileWithDebugInfo(internalName + ".class", internalName, major);
    }

    private static Entry clazz(String internalName, int major) throws Exception {
        return Entry.classFile(internalName + ".class", internalName, major);
    }

    private String render(boolean fileDetails, Artifact... dependencies) throws Exception {
        Artifact projectArtifact = new DefaultArtifact(
                "org.example", "project", "1.0", null, "pom", null, new DefaultArtifactHandler("pom"));
        MavenProject project = new MavenProject();
        project.setGroupId("org.example");
        project.setArtifactId("project");
        project.setVersion("1.0");

        DependencyNode tree = DependencyNodes.tree(projectArtifact, dependencies);
        Dependencies deps = new Dependencies(project, tree, new JarClassesAnalysis());

        RepositoryUtils repoUtils = mock(RepositoryUtils.class);
        when(repoUtils.getMavenProjectFromRepository(any(Artifact.class))).thenAnswer(invocation -> {
            Artifact artifact = invocation.getArgument(0);
            MavenProject artifactProject = new MavenProject();
            artifactProject.setName(artifact.getArtifactId());
            License license = licenses.get(artifact.getArtifactId());
            if (license != null) {
                artifactProject.setLicenses(Collections.singletonList(license));
            }
            return artifactProject;
        });

        StringWriter out = new StringWriter();
        DependenciesRenderer renderer = new DependenciesRenderer(
                new Xhtml5BaseSink(out),
                Locale.ENGLISH,
                i18n,
                new SystemStreamLog(),
                deps,
                tree,
                new DependenciesReportConfiguration(fileDetails),
                repoUtils,
                licenseMappings);
        renderer.render();
        return out.toString().replace("\r\n", "\n");
    }

    private static String cell(String align, String text) {
        return "<td style=\"text-align: " + align + ";\">" + text + "</td>";
    }

    private static String left(String text) {
        return cell("left", text);
    }

    private static String right(String text) {
        return cell("right", text);
    }

    private static String center(String text) {
        return cell("center", text);
    }

    private static String row(String... cells) {
        return String.join("\n", cells) + "</tr>";
    }

    /** Asserts that the rows occur one after the other in the report. */
    private static void assertRows(String html, String... rows) {
        StringBuilder regex = new StringBuilder();
        for (String row : rows) {
            if (regex.length() > 0) {
                regex.append("\n");
            }
            regex.append("<tr class=\"[ab]\">\n").append(Pattern.quote(row));
        }
        assertTrue(
                Pattern.compile(regex.toString()).matcher(html).find(), "rows not found:\n" + String.join("\n", rows));
    }
}
