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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.LicenseMapping;
import org.apache.maven.report.projectinfo.ProjectInfoReportUtils;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 * @author Nick Stolwijk
 * @since 2.1
 */
public class DependencyManagementRenderer extends AbstractProjectInfoRenderer {
    private final ManagementDependencies dependencies;

    private final Log log;

    private final RepositorySystemSession repoSession;

    private final List<RemoteRepository> remoteProjectRepositories;

    private final RepositorySystem repositorySystem;

    private final ArtifactHandlerManager artifactHandlerManager;

    private final RepositoryUtils repoUtils;

    private final Map<String, String> licenseMappings;

    private final Map<String, Artifact> resolvedArtifacts;

    /**
     * Default constructor
     *
     * @param sink {@link Sink}
     * @param locale {@link Locale}
     * @param i18n {@link I18N}
     * @param log {@link Log}
     * @param dependencies {@link ManagementDependencies}
     * @param repoSession the repository session
     * @param remoteProjectRepositories the list for remote repositories
     * @param repositorySystem the maven resolver {@link RepositorySystem}
     * @param artifactHandlerManager the artifact handler manager
     * @param repoUtils {@link RepositoryUtils}
     * @param licenseMappings {@link LicenseMapping}
     * @param resolvedArtifacts the resolved artifacts of the project keyed by {@code groupId:artifactId}, may be null
     */
    public DependencyManagementRenderer(
            Sink sink,
            Locale locale,
            I18N i18n,
            Log log,
            ManagementDependencies dependencies,
            RepositorySystemSession repoSession,
            List<RemoteRepository> remoteProjectRepositories,
            RepositorySystem repositorySystem,
            ArtifactHandlerManager artifactHandlerManager,
            RepositoryUtils repoUtils,
            Map<String, String> licenseMappings,
            Map<String, Artifact> resolvedArtifacts) {
        super(sink, i18n, locale);

        this.log = log;
        this.dependencies = dependencies;
        this.repoSession = repoSession;
        this.remoteProjectRepositories = remoteProjectRepositories;
        this.repositorySystem = repositorySystem;
        this.artifactHandlerManager = artifactHandlerManager;
        this.repoUtils = repoUtils;
        this.licenseMappings = licenseMappings;
        this.resolvedArtifacts = resolvedArtifacts;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    protected String getI18Nsection() {
        return "dependency-management";
    }

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
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private void renderSectionProjectDependencies() {
        startSection(getTitle());

        // collect dependencies by scope
        Map<String, List<Dependency>> dependenciesByScope = dependencies.getManagementDependenciesByScope();

        renderDependenciesForAllScopes(dependenciesByScope);

        endSection();
    }

    private void renderDependenciesForAllScopes(Map<String, List<Dependency>> dependenciesByScope) {
        renderDependenciesForScope(Artifact.SCOPE_COMPILE, dependenciesByScope.get(Artifact.SCOPE_COMPILE));
        renderDependenciesForScope(Artifact.SCOPE_RUNTIME, dependenciesByScope.get(Artifact.SCOPE_RUNTIME));
        renderDependenciesForScope(Artifact.SCOPE_TEST, dependenciesByScope.get(Artifact.SCOPE_TEST));
        renderDependenciesForScope(Artifact.SCOPE_PROVIDED, dependenciesByScope.get(Artifact.SCOPE_PROVIDED));
        renderDependenciesForScope(Artifact.SCOPE_SYSTEM, dependenciesByScope.get(Artifact.SCOPE_SYSTEM));
    }

    private String[] getDependencyTableHeader(boolean hasClassifier) {
        String groupId = getI18nString("column.groupId");
        String artifactId = getI18nString("column.artifactId");
        String version = getI18nString("column.version");
        String classifier = getI18nString("column.classifier");
        String type = getI18nString("column.type");
        String license = getI18nString("column.license");

        if (hasClassifier) {
            return new String[] {groupId, artifactId, version, classifier, type, license};
        }

        return new String[] {groupId, artifactId, version, type, license};
    }

    private void renderDependenciesForScope(String scope, List<Dependency> artifacts) {
        if (artifacts != null) {
            // can't use straight artifact comparison because we want optional last
            Collections.sort(artifacts, getDependencyComparator());

            startSection(scope);

            paragraph(getI18nString("intro." + scope));

            if (hasDependencyWithoutVersion(artifacts)) {
                paragraph(getI18nString("resolvedVersionNote"));
            }

            startTable();

            boolean hasClassifier = false;
            for (Dependency dependency : artifacts) {
                if (StringUtils.isNotEmpty(dependency.getClassifier())) {
                    hasClassifier = true;
                    break;
                }
            }

            String[] tableHeader = getDependencyTableHeader(hasClassifier);
            tableHeader(tableHeader);

            for (Dependency dependency : artifacts) {
                tableRow(getDependencyRow(dependency, hasClassifier));
            }
            endTable();

            endSection();
        }
    }

    /**
     * @param artifacts the managed dependencies of a given scope
     * @return {@code true} if at least one of them declares no version, i.e. its version will be resolved from
     *         the project dependency tree and rendered in parentheses in the version column
     */
    private boolean hasDependencyWithoutVersion(List<Dependency> artifacts) {
        for (Dependency dependency : artifacts) {
            if (StringUtils.isBlank(dependency.getVersion())) {
                return true;
            }
        }
        return false;
    }

    private String[] getDependencyRow(Dependency dependency, boolean hasClassifier) {

        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(dependency.getType());

        boolean isResolvedArtifact = false;
        String version = dependency.getVersion();
        if (StringUtils.isBlank(version)) {
            version = resolveVersion(dependency);
            isResolvedArtifact = true;
        }

        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                version,
                dependency.getScope(),
                dependency.getType(),
                dependency.getClassifier(),
                handler);

        StringBuilder licensesBuffer = new StringBuilder();
        String url = null;
        try {
            VersionRange range = VersionRange.createFromVersionSpec(version);

            if (range.getRecommendedVersion() == null) {
                // MPIR-216: no direct version but version range: need to choose one precise version
                log.debug("Resolving range for DependencyManagement on " + artifact.getId());

                List<ArtifactVersion> versions = retrieveAvailableVersions(artifact);

                // only use versions from range
                for (Iterator<ArtifactVersion> iter = versions.iterator(); iter.hasNext(); ) {
                    if (!range.containsVersion(iter.next())) {
                        iter.remove();
                    }
                }

                // select latest, assuming pom information will be the most accurate
                if (!versions.isEmpty()) {
                    ArtifactVersion maxArtifactVersion = Collections.max(versions);

                    artifact.setVersion(maxArtifactVersion.toString());
                    log.debug("DependencyManagement resolved: " + artifact.getId());
                }
            }

            MavenProject artifactProject = repoUtils.getMavenProjectFromRepository(artifact);
            url = ProjectInfoReportUtils.getProjectUrl(artifactProject);

            List<License> licenses = artifactProject.getLicenses();
            for (License license : licenses) {
                String name = license.getName();
                if (licenseMappings != null && licenseMappings.containsKey(name)) {
                    name = licenseMappings.get(name);
                }
                String licenseCell = ProjectInfoReportUtils.getArtifactIdCell(name, license.getUrl());
                if (licensesBuffer.length() > 0) {
                    licensesBuffer.append(", ");
                }
                licensesBuffer.append(licenseCell);
            }
        } catch (InvalidVersionSpecificationException e) {
            log.warn("Unable to parse version for " + artifact.getId(), e);
        } catch (VersionRangeResolutionException e) {
            log.warn("Unable to retrieve versions range for " + artifact.getId() + " from repository.", e);
        } catch (ProjectBuildingException e) {
            if (log.isDebugEnabled()) {
                log.warn("Unable to create Maven project for " + artifact.getId() + " from repository.", e);
            } else {
                log.warn("Unable to create Maven project for " + artifact.getId() + " from repository.");
            }
        }

        String artifactIdCell = ProjectInfoReportUtils.getArtifactIdCell(artifact.getArtifactId(), url);

        String versionColumn = isResolvedArtifact ? '(' + version + ')' : version;
        if (hasClassifier) {
            return new String[] {
                dependency.getGroupId(),
                artifactIdCell,
                versionColumn,
                dependency.getClassifier(),
                dependency.getType(),
                licensesBuffer.toString()
            };
        }

        return new String[] {
            dependency.getGroupId(), artifactIdCell, versionColumn, dependency.getType(), licensesBuffer.toString()
        };
    }

    /**
     * Resolve the version of a managed dependency that declares none, from the project's actual, mediated
     * dependency graph (see {@link org.apache.maven.report.projectinfo.DependencyManagementReport#resolveDependencies()}).
     *
     * @param dependency the managed dependency without a declared version
     * @return the resolved version
     * @throws InvalidArtifactRTException if the dependency could not be found in the resolved dependency graph
     */
    private String resolveVersion(Dependency dependency) {
        Artifact resolvedArtifact = null;
        if (resolvedArtifacts != null) {
            resolvedArtifact = resolvedArtifacts.get(dependency.getGroupId() + ':' + dependency.getArtifactId());
        }
        if (resolvedArtifact != null) {
            return resolvedArtifact.getVersion();
        }
        throw new InvalidArtifactRTException(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                null,
                dependency.getType(),
                "The version cannot be empty.");
    }

    /**
     * Resolves all available versions for a given artifact using the Maven Resolver API.
     *
     * @param artifact the artifact to resolve
     * @return the list of available versions
     *
     * @throws VersionRangeResolutionException if an error is present.
     */
    private List<ArtifactVersion> retrieveAvailableVersions(Artifact artifact) throws VersionRangeResolutionException {

        // "[,)" means "all versions from the beginning of time" — open range
        org.eclipse.aether.artifact.DefaultArtifact aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifactHandlerManager.getArtifactHandler(artifact.getType()).getExtension(),
                "[,)" // open version range to capture every published version
                );

        VersionRangeRequest request = new VersionRangeRequest(
                aetherArtifact,
                remoteProjectRepositories,
                null // request context — null is fine for standard resolution
                );

        VersionRangeResult result = repositorySystem.resolveVersionRange(repoSession, request);

        return result.getVersions().stream()
                .map(v -> new DefaultArtifactVersion(v.toString()))
                .collect(Collectors.toList());
    }

    private Comparator<Dependency> getDependencyComparator() {
        return new Comparator<Dependency>() {
            @Override
            public int compare(Dependency a1, Dependency a2) {
                int result = a1.getGroupId().compareTo(a2.getGroupId());
                if (result != 0) {
                    return result;
                }

                result = a1.getArtifactId().compareTo(a2.getArtifactId());
                if (result != 0) {
                    return result;
                }

                result = a1.getType().compareTo(a2.getType());
                if (result != 0) {
                    return result;
                }

                if (a1.getClassifier() == null) {
                    if (a2.getClassifier() != null) {
                        return 1;
                    }
                } else {
                    if (a2.getClassifier() != null) {
                        result = a1.getClassifier().compareTo(a2.getClassifier());
                    } else {
                        return -1;
                    }
                }

                if (result != 0) {
                    return result;
                }

                // We don't consider the version range in the comparison, just the resolved version;
                // a managed dependency without a version is resolved before we get here, but compare
                // defensively in case resolution ever leaves it null
                return Objects.toString(a1.getVersion(), "").compareTo(Objects.toString(a2.getVersion(), ""));
            }
        };
    }
}
