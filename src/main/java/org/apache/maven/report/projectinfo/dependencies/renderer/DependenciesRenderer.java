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

import javax.swing.text.html.HTML.Attribute;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.doxia.util.HtmlTools;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.LicenseMapping;
import org.apache.maven.report.projectinfo.ProjectInfoReportUtils;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependenciesRenderer.TotalCell.SummaryTableRowOrder;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClasses;
import org.apache.maven.shared.jar.classes.JarVersionedRuntime;
import org.apache.maven.shared.jar.classes.JarVersionedRuntimes;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Renderer the dependencies report.
 *
 * @version $Id$
 * @since 2.1
 */
public class DependenciesRenderer extends AbstractProjectInfoRenderer {

    /** URL for the 'icon_info_sml.gif' image */
    private static final String IMG_INFO_URL = "./images/icon_info_sml.gif";

    /** URL for the 'close.gif' image */
    private static final String IMG_CLOSE_URL = "./images/close.gif";

    private static final Set<String> JAR_SUBTYPE;

    private final DependencyNode dependencyNode;

    private final Dependencies dependencies;

    private final DependenciesReportConfiguration configuration;

    private final Log log;

    private final RepositoryUtils repoUtils;

    /** Used to format file length values */
    private final DecimalFormat fileLengthDecimalFormat;

    private final MessageFormat javaVersionFormat =
            new MessageFormat("{0,choice,0#|1.1#{0,number,0.0}|9#{0,number,0}}", Locale.ROOT);
    /**
     * @since 2.1.1
     */
    private int section;

    /** Counter for unique IDs that is consistent across generations. */
    private int idCounter = 0;

    /**
     * Will be filled with license name / set of projects.
     */
    private Map<String, Object> licenseMap = new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} */
        @Override
        public Object put(String key, Object value) {
            // handle multiple values as a set to avoid duplicates
            @SuppressWarnings("unchecked")
            SortedSet<Object> valueList = (SortedSet<Object>) get(key);
            if (valueList == null) {
                valueList = new TreeSet<>();
            }
            valueList.add(value);
            return super.put(key, valueList);
        }
    };

    private final RepositorySystem repositorySystem;

    private final ProjectBuilder projectBuilder;

    private final ProjectBuildingRequest buildingRequest;

    private final Map<String, String> licenseMappings;

    static {
        Set<String> jarSubtype = new HashSet<>();
        jarSubtype.add("jar");
        jarSubtype.add("war");
        jarSubtype.add("ear");
        jarSubtype.add("sar");
        jarSubtype.add("rar");
        jarSubtype.add("par");
        jarSubtype.add("ejb");
        JAR_SUBTYPE = Collections.unmodifiableSet(jarSubtype);
    }

    /**
     * Default constructor.
     *
     * @param sink {@link Sink}
     * @param locale {@link Locale}
     * @param i18n {@link I18N}
     * @param log {@link Log}
     * @param dependencies {@link Dependencies}
     * @param dependencyTreeNode {@link DependencyNode}
     * @param config {@link DependenciesReportConfiguration}
     * @param repoUtils {@link RepositoryUtils}
     * @param repositorySystem {@link RepositorySystem}
     * @param projectBuilder {@link ProjectBuilder}
     * @param buildingRequest {@link ProjectBuildingRequest}
     * @param licenseMappings {@link LicenseMapping}
     */
    public DependenciesRenderer(
            Sink sink,
            Locale locale,
            I18N i18n,
            Log log,
            Dependencies dependencies,
            DependencyNode dependencyTreeNode,
            DependenciesReportConfiguration config,
            RepositoryUtils repoUtils,
            RepositorySystem repositorySystem,
            ProjectBuilder projectBuilder,
            ProjectBuildingRequest buildingRequest,
            Map<String, String> licenseMappings) {
        super(sink, i18n, locale);

        this.log = log;
        this.dependencies = dependencies;
        this.dependencyNode = dependencyTreeNode;
        this.repoUtils = repoUtils;
        this.configuration = config;
        this.repositorySystem = repositorySystem;
        this.projectBuilder = projectBuilder;
        this.buildingRequest = buildingRequest;
        this.licenseMappings = licenseMappings;
        this.fileLengthDecimalFormat = new FileDecimalFormat(i18n, locale);
        this.fileLengthDecimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(locale));
    }

    @Override
    protected String getI18Nsection() {
        return "dependencies";
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    protected void renderBody() {
        // Dependencies report

        if (!dependencies.hasDependencies()) {
            startSection(getTitle());

            paragraph(getI18nString("nolist"));

            endSection();

            return;
        }

        // === Section: Project Dependencies.
        renderSectionProjectDependencies();

        // === Section: Project Transitive Dependencies.
        renderSectionProjectTransitiveDependencies();

        // === Section: Project Dependency Graph.
        renderSectionProjectDependencyGraph();

        // === Section: Licenses
        renderSectionDependencyLicenseListing();

        if (configuration.getDependencyDetailsEnabled()) {
            // === Section: Dependency File Details.
            renderSectionDependencyFileDetails();
        }
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    // workaround for MPIR-140
    // TODO Remove me when MSHARED-390 has been resolved
    @Override
    protected void startSection(String name) {
        startSection(name, name);
    }

    /**
     * Start section with a name and a specific anchor.
     *
     * @param anchor not null
     * @param name not null
     */
    // TODO Remove me when MSHARED-390 has been resolved
    protected void startSection(String name, String anchor) {
        section = section + 1;

        super.sink.anchor(HtmlTools.encodeId(anchor));
        super.sink.anchor_();

        switch (section) {
            case 1:
                sink.section1();
                sink.sectionTitle1();
                break;
            case 2:
                sink.section2();
                sink.sectionTitle2();
                break;
            case 3:
                sink.section3();
                sink.sectionTitle3();
                break;
            case 4:
                sink.section4();
                sink.sectionTitle4();
                break;
            case 5:
                sink.section5();
                sink.sectionTitle5();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }

        text(name);

        switch (section) {
            case 1:
                sink.sectionTitle1_();
                break;
            case 2:
                sink.sectionTitle2_();
                break;
            case 3:
                sink.sectionTitle3_();
                break;
            case 4:
                sink.sectionTitle4_();
                break;
            case 5:
                sink.sectionTitle5_();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }
    }

    /** {@inheritDoc} */
    // workaround for MPIR-140
    // TODO Remove me when MSHARED-390 has been resolved
    @Override
    protected void endSection() {
        switch (section) {
            case 1:
                sink.section1_();
                break;
            case 2:
                sink.section2_();
                break;
            case 3:
                sink.section3_();
                break;
            case 4:
                sink.section4_();
                break;
            case 5:
                sink.section5_();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }

        section = section - 1;

        if (section < 0) {
            throw new IllegalStateException("Too many closing sections");
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param withClassifier <code>true</code> to include the classifier column, <code>false</code> otherwise.
     * @param withOptional <code>true</code> to include the optional column, <code>false</code> otherwise.
     * @return the dependency table header with/without classifier/optional column
     * @see #renderArtifactRow(Artifact, boolean, boolean)
     */
    private String[] getDependencyTableHeader(boolean withClassifier, boolean withOptional) {
        String groupId = getI18nString("column.groupId");
        String artifactId = getI18nString("column.artifactId");
        String version = getI18nString("column.version");
        String classifier = getI18nString("column.classifier");
        String type = getI18nString("column.type");
        String license = getI18nString("column.licenses");
        String optional = getI18nString("column.optional");

        if (withClassifier) {
            if (withOptional) {
                return new String[] {groupId, artifactId, version, classifier, type, license, optional};
            }

            return new String[] {groupId, artifactId, version, classifier, type, license};
        }

        if (withOptional) {
            return new String[] {groupId, artifactId, version, type, license, optional};
        }

        return new String[] {groupId, artifactId, version, type, license};
    }

    private void renderSectionProjectDependencies() {
        startSection(getTitle());

        // collect dependencies by scope
        Map<String, List<Artifact>> dependenciesByScope = dependencies.getDependenciesByScope(false);

        renderDependenciesForAllScopes(dependenciesByScope, false);

        endSection();
    }

    /**
     * @param dependenciesByScope map with supported scopes as key and a list of <code>Artifact</code> as values.
     * @param isTransitive <code>true</code> if it is transitive dependencies rendering.
     * @see Artifact#SCOPE_COMPILE
     * @see Artifact#SCOPE_PROVIDED
     * @see Artifact#SCOPE_RUNTIME
     * @see Artifact#SCOPE_SYSTEM
     * @see Artifact#SCOPE_TEST
     */
    private void renderDependenciesForAllScopes(Map<String, List<Artifact>> dependenciesByScope, boolean isTransitive) {
        renderDependenciesForScope(
                Artifact.SCOPE_COMPILE, dependenciesByScope.get(Artifact.SCOPE_COMPILE), isTransitive);
        renderDependenciesForScope(
                Artifact.SCOPE_RUNTIME, dependenciesByScope.get(Artifact.SCOPE_RUNTIME), isTransitive);
        renderDependenciesForScope(Artifact.SCOPE_TEST, dependenciesByScope.get(Artifact.SCOPE_TEST), isTransitive);
        renderDependenciesForScope(
                Artifact.SCOPE_PROVIDED, dependenciesByScope.get(Artifact.SCOPE_PROVIDED), isTransitive);
        renderDependenciesForScope(Artifact.SCOPE_SYSTEM, dependenciesByScope.get(Artifact.SCOPE_SYSTEM), isTransitive);
    }

    private void renderSectionProjectTransitiveDependencies() {
        Map<String, List<Artifact>> dependenciesByScope = dependencies.getDependenciesByScope(true);

        startSection(getI18nString("transitive.title"));

        if (dependenciesByScope.values().isEmpty()) {
            paragraph(getI18nString("transitive.nolist"));
        } else {
            paragraph(getI18nString("transitive.intro"));

            renderDependenciesForAllScopes(dependenciesByScope, true);
        }

        endSection();
    }

    private void renderSectionProjectDependencyGraph() {
        startSection(getI18nString("graph.title"));

        // === SubSection: Dependency Tree
        renderSectionDependencyTree();

        endSection();
    }

    private void renderSectionDependencyTree() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("      function toggleDependencyDetails( divId, imgId )");
        pw.println("      {");
        pw.println("        var div = document.getElementById( divId );");
        pw.println("        var img = document.getElementById( imgId );");
        pw.println("        if( div.style.display == '' )");
        pw.println("        {");
        pw.println("          div.style.display = 'none';");
        pw.printf("          img.src='%s';%n", IMG_INFO_URL);
        pw.printf("          img.alt='%s';%n", getI18nString("graph.icon.information"));
        pw.println("        }");
        pw.println("        else");
        pw.println("        {");
        pw.println("          div.style.display = '';");
        pw.printf("          img.src='%s';%n", IMG_CLOSE_URL);
        pw.printf("          img.alt='%s';%n", getI18nString("graph.icon.close"));
        pw.println("        }");
        pw.println("      }");

        javaScript(sw.toString());

        // for Dependencies Graph Tree
        startSection(getI18nString("graph.tree.title"));

        sink.list();
        printDependencyListing(dependencyNode);
        sink.list_();

        endSection();
    }

    private void renderSectionDependencyFileDetails() {
        startSection(getI18nString("file.details.title"));

        List<Artifact> alldeps = dependencies.getAllDependencies();
        Collections.sort(alldeps, getArtifactComparator());

        resolveAtrifacts(alldeps);

        // i18n
        String filename = getI18nString("file.details.column.file");
        String size = getI18nString("file.details.column.size");
        String entries = getI18nString("file.details.column.entries");
        String classes = getI18nString("file.details.column.classes");
        String packages = getI18nString("file.details.column.packages");
        String javaVersion = getI18nString("file.details.column.javaVersion");
        String debugInformation = getI18nString("file.details.column.debuginformation");
        String debugInformationTitle = getI18nString("file.details.columntitle.debuginformation");
        String debugInformationCellYes = getI18nString("file.details.cell.debuginformation.yes");
        String debugInformationCellNo = getI18nString("file.details.cell.debuginformation.no");
        String aSealed = getI18nString("file.details.column.sealed");
        String sealedCellYes = getI18nString("file.details.cell.sealed.yes");
        String sealedCellNo = getI18nString("file.details.cell.sealed.no");

        int[] justification = new int[] {
            Sink.JUSTIFY_LEFT,
            Sink.JUSTIFY_RIGHT,
            Sink.JUSTIFY_RIGHT,
            Sink.JUSTIFY_RIGHT,
            Sink.JUSTIFY_RIGHT,
            Sink.JUSTIFY_CENTER,
            Sink.JUSTIFY_CENTER,
            Sink.JUSTIFY_CENTER
        };

        startTable(justification, false);

        TotalCell totaldeps = new TotalCell();
        TotalCell totaldepsize = new TotalCell(fileLengthDecimalFormat);
        TotalCell totalentries = new TotalCell();
        TotalCell totalclasses = new TotalCell();
        TotalCell totalpackages = new TotalCell();
        double highestTestJavaVersion = 0.0;
        double highestNonTestJavaVersion = 0.0;
        TotalCell totalDebugInformation = new TotalCell();
        TotalCell totalsealed = new TotalCell();

        boolean hasSealed = hasSealed(alldeps);

        // Table header
        String[] tableHeader;
        String[] tableHeaderTitles;
        if (hasSealed) {
            tableHeader =
                    new String[] {filename, size, entries, classes, packages, javaVersion, debugInformation, aSealed};
            tableHeaderTitles = new String[] {null, null, null, null, null, null, debugInformationTitle, null};
        } else {
            tableHeader = new String[] {filename, size, entries, classes, packages, javaVersion, debugInformation};
            tableHeaderTitles = new String[] {null, null, null, null, null, null, debugInformationTitle};
        }
        tableHeader(tableHeader, tableHeaderTitles);

        // Table rows
        for (Artifact artifact : alldeps) {
            if (artifact.getFile() == null) {
                log.warn("Artifact " + artifact.getId() + " has no file"
                        + " and won't be listed in dependency files details.");
                continue;
            }
            if (SummaryTableRowOrder.fromScope(artifact.getScope()) == null) {
                log.warn("Artifact " + artifact.getId() + " has invalid scope"
                        + " and won't be listed in dependency files details.");
                continue;
            }

            File artifactFile = dependencies.getFile(artifact);

            totaldeps.incrementTotal(artifact.getScope());
            totaldepsize.addTotal(artifactFile.length(), artifact.getScope());

            if (JAR_SUBTYPE.contains(artifact.getType().toLowerCase())) {
                try {
                    JarData jarData = dependencies.getJarDependencyDetails(artifact);

                    totalentries.addTotal(jarData.getNumEntries(), artifact.getScope());
                    totalclasses.addTotal(jarData.getNumClasses(), artifact.getScope());
                    totalpackages.addTotal(jarData.getNumPackages(), artifact.getScope());

                    String jdkRevisionCellValue = jarData.getJdkRevision();
                    String debugInformationCellValue = null;
                    String sealedCellValue = null;

                    if (jdkRevisionCellValue != null) {
                        try {
                            double jdkRevision = Double.parseDouble(jdkRevisionCellValue);
                            boolean isTestScope = Artifact.SCOPE_TEST.equalsIgnoreCase(artifact.getScope());
                            if (isTestScope) {
                                highestTestJavaVersion = Math.max(highestTestJavaVersion, jdkRevision);
                            } else {
                                highestNonTestJavaVersion = Math.max(highestNonTestJavaVersion, jdkRevision);
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }

                        debugInformationCellValue = debugInformationCellNo;
                        if (jarData.isDebugPresent()) {
                            debugInformationCellValue = debugInformationCellYes;
                            totalDebugInformation.incrementTotal(artifact.getScope());
                        }

                        sealedCellValue = sealedCellNo;
                        if (jarData.isSealed()) {
                            sealedCellValue = sealedCellYes;
                            totalsealed.incrementTotal(artifact.getScope());
                        }
                    }

                    String name = artifactFile.getName();
                    String fileLength = fileLengthDecimalFormat.format(artifactFile.length());

                    if (artifactFile.isDirectory()) {
                        File parent = artifactFile.getParentFile();
                        name = parent.getParentFile().getName() + '/' + parent.getName() + '/' + artifactFile.getName();
                        fileLength = "-";
                    }

                    if (jarData.isMultiRelease()) {
                        String htmlBullet = "&#160;&#160;&#160;&#x2022; ";
                        String rootTag = htmlBullet + getI18nString("file.details.multirelease.root");
                        String versionedTag = htmlBullet + getI18nString("file.details.multirelease.versioned");

                        // general jar information row
                        tableRow(hasSealed, new String[] {
                            name, fileLength, String.valueOf(jarData.getNumEntries()), "", "", "", "", sealedCellValue
                        });

                        JarVersionedRuntimes versionedRuntimes = jarData.getVersionedRuntimes();
                        Collection<JarVersionedRuntime> versionedRuntimeList =
                                versionedRuntimes.getVersionedRuntimeMap().values();

                        // workaround to count the number of root content entries
                        // TODO: rework this when MSHARED-1411 is fixed
                        Integer versionedNumEntries = versionedRuntimeList.stream()
                                .map(versionedRuntime ->
                                        versionedRuntime.getEntries().size())
                                .reduce(0, Integer::sum);
                        Integer rootContentNumEntries = jarData.getNumEntries() - versionedNumEntries;

                        // root content information row
                        tableRow(hasSealed, new String[] {
                            rootTag,
                            "",
                            String.valueOf(rootContentNumEntries),
                            String.valueOf(jarData.getNumClasses()),
                            String.valueOf(jarData.getNumPackages()),
                            jdkRevisionCellValue,
                            debugInformationCellValue,
                            ""
                        });

                        for (JarVersionedRuntime versionedRuntime : versionedRuntimeList) {
                            JarClasses rtJarClasses = versionedRuntime.getJarClasses();

                            debugInformationCellValue =
                                    rtJarClasses.isDebugPresent() ? debugInformationCellYes : debugInformationCellNo;

                            tableRow(hasSealed, new String[] {
                                versionedTag,
                                "",
                                String.valueOf(versionedRuntime.getEntries().size()),
                                String.valueOf(rtJarClasses.getClassNames().size()),
                                String.valueOf(rtJarClasses.getPackages().size()),
                                rtJarClasses.getJdkRevision(),
                                debugInformationCellValue,
                                ""
                            });
                        }
                    } else {
                        tableRow(hasSealed, new String[] {
                            name,
                            fileLength,
                            String.valueOf(jarData.getNumEntries()),
                            String.valueOf(jarData.getNumClasses()),
                            String.valueOf(jarData.getNumPackages()),
                            jdkRevisionCellValue,
                            debugInformationCellValue,
                            sealedCellValue
                        });
                    }
                } catch (IOException e) {
                    createExceptionInfoTableRow(artifact, artifactFile, e, hasSealed);
                }
            } else {
                tableRow(hasSealed, new String[] {
                    artifactFile.getName(),
                    fileLengthDecimalFormat.format(artifactFile.length()),
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                });
            }
        }

        // Total raws
        tableHeader[0] = getI18nString("file.details.total");
        tableHeader(tableHeader);

        justification[0] = Sink.JUSTIFY_RIGHT;
        justification[6] = Sink.JUSTIFY_RIGHT;

        // calculate rowspan attr
        int rowspan = computeRowspan(totaldeps);

        if (rowspan > 1) {
            boolean insertRowspanAttr = false;
            int column = 5; // Java Version's column
            for (SummaryTableRowOrder currentRow : SummaryTableRowOrder.values()) {
                if (currentRow.getTotal(totaldeps) > 0) {
                    int i = currentRow.ordinal();
                    boolean alreadyInsertedRowspanAttr = insertRowspanAttr
                            && (SummaryTableRowOrder.COMPILE_SCOPE.ordinal() < i
                                    && i <= SummaryTableRowOrder.SYSTEM_SCOPE.ordinal());
                    insertRowspanAttr = (SummaryTableRowOrder.COMPILE_SCOPE.ordinal() <= i
                            && i <= SummaryTableRowOrder.SYSTEM_SCOPE.ordinal());
                    justification[column] = (insertRowspanAttr && alreadyInsertedRowspanAttr)
                            ? justification[column + 1]
                            : Sink.JUSTIFY_CENTER;
                    tableRowWithRowspan(
                            hasSealed, insertRowspanAttr, alreadyInsertedRowspanAttr, column, rowspan, new String[] {
                                totaldeps.getTotalString(currentRow),
                                totaldepsize.getTotalString(currentRow),
                                totalentries.getTotalString(currentRow),
                                totalclasses.getTotalString(currentRow),
                                totalpackages.getTotalString(currentRow),
                                currentRow.formatMaxJavaVersionForScope(
                                        javaVersionFormat, highestTestJavaVersion, highestNonTestJavaVersion),
                                totalDebugInformation.getTotalString(currentRow),
                                totalsealed.getTotalString(currentRow)
                            });
                }
            }
        } else {
            for (SummaryTableRowOrder currentRow : SummaryTableRowOrder.values()) {
                if (currentRow.getTotal(totaldeps) > 0) {
                    tableRow(hasSealed, new String[] {
                        totaldeps.getTotalString(currentRow),
                        totaldepsize.getTotalString(currentRow),
                        totalentries.getTotalString(currentRow),
                        totalclasses.getTotalString(currentRow),
                        totalpackages.getTotalString(currentRow),
                        currentRow.formatMaxJavaVersionForScope(
                                javaVersionFormat, highestTestJavaVersion, highestNonTestJavaVersion),
                        totalDebugInformation.getTotalString(currentRow),
                        totalsealed.getTotalString(currentRow)
                    });
                }
            }
        }

        endTable();
        endSection();
    }

    private int computeRowspan(TotalCell totaldeps) {
        int rowspan = 0;
        for (int i = SummaryTableRowOrder.COMPILE_SCOPE.ordinal();
                i <= SummaryTableRowOrder.SYSTEM_SCOPE.ordinal();
                i++) {
            SummaryTableRowOrder currentRow = SummaryTableRowOrder.values()[i];
            if (currentRow.getTotal(totaldeps) > 0) {
                rowspan++;
            }
        }
        return rowspan;
    }

    // Almost as same as in the abstract class but includes the title attribute
    private void tableHeader(String[] content, String[] titles) {
        sink.tableRow();

        if (content != null) {
            if (titles != null && content.length != titles.length) {
                throw new IllegalArgumentException("Length of title array must equal the length of the content array");
            }

            for (int i = 0; i < content.length; i++) {
                if (titles != null) {
                    tableHeaderCell(content[i], titles[i]);
                } else {
                    tableHeaderCell(content[i]);
                }
            }
        }

        sink.tableRow_();
    }

    private void tableHeaderCell(String text, String title) {
        if (title != null) {
            sink.tableHeaderCell(new SinkEventAttributeSet(SinkEventAttributes.TITLE, title));
        } else {
            sink.tableHeaderCell();
        }

        text(text);

        sink.tableHeaderCell_();
    }

    private void tableRowWithRowspan(
            boolean fullRow, boolean insert, boolean alreadyInserted, int contentIndex, int rowspan, String[] content) {
        sink.tableRow();

        int count = fullRow ? content.length : (content.length - 1);

        for (int i = 0; i < count; i++) {
            if (i == contentIndex && insert) {
                if (!alreadyInserted) {
                    SinkEventAttributes att = new SinkEventAttributeSet();
                    att.addAttribute(Attribute.ROWSPAN, rowspan);
                    att.addAttribute(Attribute.STYLE, "vertical-align: middle;");
                    sink.tableCell(att);
                    text(content[i]);
                    sink.tableCell_();
                }
            } else {
                tableCell(content[i]);
            }
        }

        sink.tableRow_();
    }

    private void tableRow(boolean fullRow, String[] content) {
        sink.tableRow();

        int count = fullRow ? content.length : (content.length - 1);

        for (int i = 0; i < count; i++) {
            tableCell(content[i]);
        }

        sink.tableRow_();
    }

    private void createExceptionInfoTableRow(Artifact artifact, File artifactFile, Exception e, boolean hasSealed) {
        tableRow(
                hasSealed,
                new String[] {artifact.getId(), artifactFile.getAbsolutePath(), e.getMessage(), "", "", "", "", ""});
    }

    private void renderSectionDependencyLicenseListing() {
        startSection(getI18nString("graph.tables.licenses"));
        printGroupedLicenses();
        endSection();
    }

    private void renderDependenciesForScope(String scope, List<Artifact> artifacts, boolean isTransitive) {
        if (artifacts != null) {
            boolean withClassifier = hasClassifier(artifacts);
            boolean withOptional = hasOptional(artifacts);
            String[] tableHeader = getDependencyTableHeader(withClassifier, withOptional);

            // can't use straight artifact comparison because we want optional last
            Collections.sort(artifacts, getArtifactComparator());

            String anchorByScope = isTransitive
                    ? getI18nString("transitive.title") + "_" + scope
                    : getI18nString("title") + "_" + scope;
            startSection(scope, anchorByScope);

            paragraph(getI18nString("intro." + scope));

            startTable();
            tableHeader(tableHeader);
            for (Artifact artifact : artifacts) {
                renderArtifactRow(artifact, withClassifier, withOptional);
            }
            endTable();

            endSection();
        }
    }

    private Comparator<Artifact> getArtifactComparator() {
        return new Comparator<Artifact>() {
            public int compare(Artifact a1, Artifact a2) {
                // put optional last
                if (a1.isOptional() && !a2.isOptional()) {
                    return +1;
                } else if (!a1.isOptional() && a2.isOptional()) {
                    return -1;
                } else {
                    return a1.compareTo(a2);
                }
            }
        };
    }

    /**
     * @param artifact not null
     * @param withClassifier <code>true</code> to include the classifier column, <code>false</code> otherwise.
     * @param withOptional <code>true</code> to include the optional column, <code>false</code> otherwise.
     * @see #getDependencyTableHeader(boolean, boolean)
     */
    private void renderArtifactRow(Artifact artifact, boolean withClassifier, boolean withOptional) {
        String isOptional =
                artifact.isOptional() ? getI18nString("column.isOptional") : getI18nString("column.isNotOptional");

        String url = ProjectInfoReportUtils.getArtifactUrl(repositorySystem, artifact, projectBuilder, buildingRequest);
        String artifactIdCell = ProjectInfoReportUtils.getArtifactIdCell(artifact.getArtifactId(), url);

        MavenProject artifactProject;
        StringBuilder sb = new StringBuilder();
        try {
            artifactProject = repoUtils.getMavenProjectFromRepository(artifact);

            List<License> licenses = artifactProject.getLicenses();
            for (License license : licenses) {
                String name = license.getName();
                if (licenseMappings != null && licenseMappings.containsKey(name)) {
                    name = licenseMappings.get(name);
                }
                sb.append(ProjectInfoReportUtils.getArtifactIdCell(name, license.getUrl()));
            }
        } catch (ProjectBuildingException e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to create Maven project from repository for artifact '" + artifact.getId() + "'", e);
            } else {
                log.info("Unable to create Maven project from repository for artifact '" + artifact.getId()
                        + "', for more information run with -X");
            }
        }

        String[] content;
        if (withClassifier) {
            content = new String[] {
                artifact.getGroupId(),
                artifactIdCell,
                artifact.getVersion(),
                artifact.getClassifier(),
                artifact.getType(),
                sb.toString(),
                isOptional
            };
        } else {
            content = new String[] {
                artifact.getGroupId(),
                artifactIdCell,
                artifact.getVersion(),
                artifact.getType(),
                sb.toString(),
                isOptional
            };
        }

        tableRow(withOptional, content);
    }

    private void printDependencyListing(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        String id = artifact.getId();
        String dependencyDetailId = "_dep" + idCounter++;
        String imgId = "_img" + idCounter++;

        sink.listItem();

        sink.text(id + (StringUtils.isNotEmpty(artifact.getScope()) ? " (" + artifact.getScope() + ") " : " "));

        String javascript = String.format(
                "<img id=\"%s\" src=\"%s\" alt=\"%s\""
                        + " onclick=\"toggleDependencyDetails( '%s', '%s' );\""
                        + " style=\"cursor: pointer; vertical-align: text-bottom;\" />",
                imgId, IMG_INFO_URL, getI18nString("graph.icon.information"), dependencyDetailId, imgId);

        sink.rawText(javascript);

        printDescriptionsAndURLs(node, dependencyDetailId);

        if (!node.getChildren().isEmpty()) {
            boolean toBeIncluded = false;
            List<DependencyNode> subList = new ArrayList<>();
            for (DependencyNode dep : node.getChildren()) {
                if (dependencies.getAllDependencies().contains(dep.getArtifact())) {
                    subList.add(dep);
                    toBeIncluded = true;
                }
            }

            if (toBeIncluded) {
                sink.list();
                for (DependencyNode dep : subList) {
                    printDependencyListing(dep);
                }
                sink.list_();
            }
        }

        sink.listItem_();
    }

    private void printDescriptionsAndURLs(DependencyNode node, String uid) {
        Artifact artifact = node.getArtifact();
        String id = artifact.getId();
        String unknownLicenseMessage = getI18nString("graph.tables.unknown");

        sink.rawText("<div id=\"" + uid + "\" style=\"display:none\">");

        if (!Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
            try {
                MavenProject artifactProject = repoUtils.getMavenProjectFromRepository(artifact);
                String artifactDescription = artifactProject.getDescription();
                String artifactUrl = artifactProject.getUrl();
                String artifactName = artifactProject.getName();

                List<License> licenses = artifactProject.getLicenses();

                startTable();

                sink.tableRow();
                sink.tableHeaderCell();
                sink.text(artifactName);
                sink.tableHeaderCell_();
                sink.tableRow_();

                sink.tableRow();
                sink.tableCell();

                sink.paragraph();
                sink.bold();
                sink.text(getI18nString("column.description") + ": ");
                sink.bold_();
                if (artifactDescription != null && !artifactDescription.isEmpty()) {
                    sink.text(artifactDescription);
                } else {
                    sink.text(getI18nString("index", "nodescription"));
                }
                sink.paragraph_();

                if (artifactUrl != null && !artifactUrl.isEmpty()) {
                    sink.paragraph();
                    sink.bold();
                    sink.text(getI18nString("column.url") + ": ");
                    sink.bold_();
                    if (ProjectInfoReportUtils.isArtifactUrlValid(artifactUrl)) {
                        sink.link(artifactUrl);
                        sink.text(artifactUrl);
                        sink.link_();
                    } else {
                        sink.text(artifactUrl);
                    }
                    sink.paragraph_();
                }

                sink.paragraph();
                sink.bold();
                sink.text(getI18nString("licenses", "title") + ": ");
                sink.bold_();
                if (!licenses.isEmpty()) {

                    for (Iterator<License> it = licenses.iterator(); it.hasNext(); ) {
                        License license = it.next();

                        String licenseName = license.getName();
                        if (licenseMappings != null && licenseMappings.containsKey(licenseName)) {
                            licenseName = licenseMappings.get(licenseName);
                        }
                        if (licenseName == null || licenseName.isEmpty()) {
                            licenseName = getI18nString("unnamed");
                        }

                        String licenseUrl = license.getUrl();

                        if (licenseUrl != null) {
                            sink.link(licenseUrl);
                        }
                        sink.text(licenseName);

                        if (licenseUrl != null) {
                            sink.link_();
                        }

                        if (it.hasNext()) {
                            sink.text(", ");
                        }

                        licenseMap.put(licenseName, artifactName);
                    }
                } else {
                    sink.text(getI18nString("licenses", "nolicense"));

                    licenseMap.put(unknownLicenseMessage, artifactName);
                }
                sink.paragraph_();

                sink.tableCell_();
                sink.tableRow_();

                endTable();
            } catch (ProjectBuildingException e) {
                sink.text(getI18nString("index", "nodescription"));
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Unable to create Maven project from repository for artifact '" + artifact.getId() + "'",
                            e);
                } else {
                    log.info("Unable to create Maven project from repository for artifact '" + artifact.getId()
                            + "', for more information run with -X");
                }
            }
        } else {
            startTable();

            sink.tableRow();
            sink.tableHeaderCell();
            sink.text(id);
            sink.tableHeaderCell_();
            sink.tableRow_();

            sink.tableRow();
            sink.tableCell();

            sink.paragraph();
            sink.bold();
            sink.text(getI18nString("column.description") + ": ");
            sink.bold_();
            sink.text(getI18nString("index", "nodescription"));
            sink.paragraph_();

            if (artifact.getFile() != null) {
                sink.paragraph();
                sink.bold();
                sink.text(getI18nString("column.url") + ": ");
                sink.bold_();
                sink.text(artifact.getFile().getAbsolutePath());
                sink.paragraph_();
            }

            sink.tableCell_();
            sink.tableRow_();

            endTable();
        }

        sink.rawText("</div>");
    }

    private void printGroupedLicenses() {
        for (Map.Entry<String, Object> entry : licenseMap.entrySet()) {
            String licenseName = entry.getKey();
            if (licenseName == null || licenseName.isEmpty()) {
                licenseName = getI18nString("unnamed");
            }

            sink.paragraph();
            sink.bold();
            sink.text(licenseName);
            sink.text(": ");
            sink.bold_();

            @SuppressWarnings("unchecked")
            SortedSet<String> projects = (SortedSet<String>) entry.getValue();

            for (Iterator<String> iterator = projects.iterator(); iterator.hasNext(); ) {
                String projectName = iterator.next();
                sink.text(projectName);
                if (iterator.hasNext()) {
                    sink.text(", ");
                }
            }

            sink.paragraph_();
        }
    }

    /**
     * Resolves all given artifacts with {@link RepositoryUtils}.
     *
     ** @param artifacts not null
     */
    private void resolveAtrifacts(List<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            // TODO site:run Why do we need to resolve this...
            if (artifact.getFile() == null) {
                if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
                    // can not resolve system scope artifact file
                    continue;
                }

                try {
                    repoUtils.resolve(artifact);
                } catch (ArtifactResolverException e) {
                    log.error("Artifact " + artifact.getId() + " can't be resolved.", e);
                    continue;
                }

                if (artifact.getFile() == null) {
                    log.error("Artifact " + artifact.getId() + " has no file, even after resolution.");
                }
            }
        }
    }

    /**
     * @param artifacts not null
     * @return <code>true</code> if one artifact in the list has a classifier, <code>false</code> otherwise.
     */
    private boolean hasClassifier(List<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            if (StringUtils.isNotEmpty(artifact.getClassifier())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param artifacts not null
     * @return <code>true</code> if one artifact in the list is optional, <code>false</code> otherwise.
     */
    private boolean hasOptional(List<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            if (artifact.isOptional()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param artifacts not null
     * @return <code>true</code> if one artifact in the list is sealed, <code>false</code> otherwise.
     */
    private boolean hasSealed(List<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null
                    && JAR_SUBTYPE.contains(artifact.getType().toLowerCase())) {
                try {
                    JarData jarDetails = dependencies.getJarDependencyDetails(artifact);
                    if (jarDetails.isSealed()) {
                        return true;
                    }
                } catch (IOException e) {
                    log.error("Artifact " + artifact.getId() + " caused IOException: " + e.getMessage(), e);
                }
            }
        }
        return false;
    }

    // CHECKSTYLE_OFF: LineLength
    /**
     * Formats file length with the associated <a href="https://en.wikipedia.org/wiki/Metric_prefix">SI</a> prefix
     * (GB, MB, kB) and using the pattern <code>###0.#</code> by default.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Metric_prefix">https://en.wikipedia.org/wiki/Metric_prefix</a>
     * @see <a href="https://en.wikipedia.org/wiki/Binary_prefix">https://en.wikipedia.org/wiki/Binary_prefix</a>
     * @see <a
     *      href="https://en.wikipedia.org/wiki/Octet_%28computing%29">https://en.wikipedia.org/wiki/Octet_(computing)</a>
     */
    // CHECKSTYLE_ON: LineLength
    static class FileDecimalFormat extends DecimalFormat {
        private static final long serialVersionUID = 4062503546523610081L;

        private final I18N i18n;

        private final Locale locale;

        /**
         * Default constructor
         *
         * @param i18n
         * @param locale
         */
        FileDecimalFormat(I18N i18n, Locale locale) {
            super("###0.#");

            this.i18n = i18n;
            this.locale = locale;
        }

        /** {@inheritDoc} */
        @Override
        public StringBuffer format(long fs, StringBuffer result, FieldPosition fieldPosition) {
            if (fs > 1000 * 1000 * 1000) {
                result = super.format((float) fs / (1000 * 1000 * 1000), result, fieldPosition);
                result.append(" ").append(getString("report.dependencies.file.details.column.size.gb"));
                return result;
            }

            if (fs > 1000 * 1000) {
                result = super.format((float) fs / (1000 * 1000), result, fieldPosition);
                result.append(" ").append(getString("report.dependencies.file.details.column.size.mb"));
                return result;
            }

            result = super.format((float) fs / 1000, result, fieldPosition);
            result.append(" ").append(getString("report.dependencies.file.details.column.size.kb"));
            return result;
        }

        private String getString(String key) {
            return i18n.getString("project-info-reports", locale, key);
        }
    }

    /**
     * Combine total and total by scope in a cell.
     */
    static class TotalCell {
        public enum SummaryTableRowOrder {
            // Do not change the physical order of these values
            TOTALS {
                @Override
                public void addTotal(TotalCell cell, long value) {
                    cell.total += value;
                }

                @Override
                public long getTotal(TotalCell cell) {
                    return cell.total;
                }

                @Override
                protected String formatMaxJavaVersionForScope(
                        MessageFormat javaVersionFormat,
                        double highestTestJavaVersion,
                        double highestNonTestJavaVersion) {
                    double highestJavaVersion = Math.max(highestTestJavaVersion, highestNonTestJavaVersion);
                    return javaVersionFormat.format(new Object[] {highestJavaVersion});
                }
            },
            COMPILE_SCOPE(Artifact.SCOPE_COMPILE) {
                @Override
                public void addTotal(TotalCell cell, long value) {
                    cell.totalCompileScope += value;
                }

                @Override
                public long getTotal(TotalCell cell) {
                    return cell.totalCompileScope;
                }
            },
            RUNTIME_SCOPE(Artifact.SCOPE_RUNTIME) {
                @Override
                public void addTotal(TotalCell cell, long value) {
                    cell.totalRuntimeScope += value;
                }

                @Override
                public long getTotal(TotalCell cell) {
                    return cell.totalRuntimeScope;
                }
            },
            PROVIDED_SCOPE(Artifact.SCOPE_PROVIDED) {
                @Override
                public void addTotal(TotalCell cell, long value) {
                    cell.totalProvidedScope += value;
                }

                @Override
                public long getTotal(TotalCell cell) {
                    return cell.totalProvidedScope;
                }
            },
            SYSTEM_SCOPE(Artifact.SCOPE_SYSTEM) {
                @Override
                public void addTotal(TotalCell cell, long value) {
                    cell.totalSystemScope += value;
                }

                @Override
                public long getTotal(TotalCell cell) {
                    return cell.totalSystemScope;
                }
            },
            TEST_SCOPE(Artifact.SCOPE_TEST) {
                @Override
                public void addTotal(TotalCell cell, long value) {
                    cell.totalTestScope += value;
                }

                @Override
                public long getTotal(TotalCell cell) {
                    return cell.totalTestScope;
                }

                @Override
                protected String formatMaxJavaVersionForScope(
                        MessageFormat javaVersionFormat,
                        double highestTestJavaVersion,
                        double highestNonTestJavaVersion) {
                    return javaVersionFormat.format(new Object[] {highestTestJavaVersion});
                }
            };

            private static final Map<String, SummaryTableRowOrder> MAP_BY_SCOPE = new HashMap<>();

            static {
                // scope string => enum mapping
                for (SummaryTableRowOrder e : SummaryTableRowOrder.values()) {
                    MAP_BY_SCOPE.put(e.getScope(), e);
                }
            }

            public static SummaryTableRowOrder fromScope(String scope) {
                return MAP_BY_SCOPE.get(scope);
            }

            private String scope;

            SummaryTableRowOrder() {
                this(null);
            }

            SummaryTableRowOrder(String scope) {
                this.scope = scope;
            }

            public String getScope() {
                return this.scope;
            }

            protected String formatMaxJavaVersionForScope(
                    MessageFormat javaVersionFormat, double highestTestJavaVersion, double highestNonTestJavaVersion) {
                return javaVersionFormat.format(new Object[] {highestNonTestJavaVersion});
            }

            public abstract void addTotal(TotalCell cell, long value);

            public abstract long getTotal(TotalCell cell);
        }

        DecimalFormat decimalFormat;

        long total = 0;

        long totalCompileScope = 0;

        long totalTestScope = 0;

        long totalRuntimeScope = 0;

        long totalProvidedScope = 0;

        long totalSystemScope = 0;

        TotalCell() {}

        TotalCell(DecimalFormat decimalFormat) {
            this.decimalFormat = decimalFormat;
        }

        void incrementTotal(String scope) {
            addTotal(1, scope);
        }

        String getTotalString(SummaryTableRowOrder currentRow) {
            long totalString = currentRow.getTotal(this);

            if (totalString <= 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            if (currentRow.compareTo(SummaryTableRowOrder.COMPILE_SCOPE) >= 0) {
                sb.append(currentRow.getScope()).append(": ");
            }
            if (decimalFormat != null) {
                sb.append(decimalFormat.format(currentRow.getTotal(this)));
            } else {
                sb.append(currentRow.getTotal(this));
            }

            return sb.toString();
        }

        void addTotal(long add, String scope) {
            SummaryTableRowOrder.TOTALS.addTotal(this, add);
            SummaryTableRowOrder currentRow = SummaryTableRowOrder.fromScope(scope);
            currentRow.addTotal(this, add);
        }

        /** {@inheritDoc} */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (decimalFormat != null) {
                sb.append(decimalFormat.format(total));
            } else {
                sb.append(total);
            }

            sb.append(" (");

            boolean needSeparator = false;
            for (int i = SummaryTableRowOrder.COMPILE_SCOPE.ordinal();
                    i < SummaryTableRowOrder.TEST_SCOPE.ordinal();
                    i++) {
                SummaryTableRowOrder currentRow = SummaryTableRowOrder.values()[i];
                if (currentRow.getTotal(this) > 0) {
                    if (needSeparator) {
                        sb.append(", ");
                    }
                    sb.append(getTotalString(currentRow));
                    needSeparator = true;
                }
            }

            sb.append(")");

            return sb.toString();
        }
    }
}
