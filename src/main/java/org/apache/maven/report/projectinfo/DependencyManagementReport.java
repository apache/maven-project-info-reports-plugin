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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependencyManagementRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;

/**
 * Generates the Project Dependency Management report.
 *
 * @author Nick Stolwijk
 * @since 2.1
 */
@Mojo(name = "dependency-management", requiresDependencyResolution = ResolutionScope.TEST)
public class DependencyManagementReport extends AbstractProjectInfoReport {

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Lazy instantiation for management dependencies.
     */
    private ManagementDependencies managementDependencies;

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    private final RepositoryUtils repoUtils;

    private final ArtifactHandlerManager artifactHandlerManager;

    private final ProjectDependenciesResolver projectDependenciesResolver;

    @Inject
    protected DependencyManagementReport(
            RepositorySystem repositorySystem,
            ArtifactHandlerManager artifactHandlerManager,
            I18N i18n,
            ProjectBuilder projectBuilder,
            RepositoryUtils repoUtils,
            ProjectDependenciesResolver projectDependenciesResolver) {
        super(repositorySystem, i18n, projectBuilder);
        this.artifactHandlerManager = artifactHandlerManager;
        this.repoUtils = repoUtils;
        this.projectDependenciesResolver = projectDependenciesResolver;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        boolean result = super.canGenerateReport();
        if (result && skipEmptyReport) {
            result = getManagementDependencies().hasDependencies();
        }

        return result;
    }

    @Override
    public void executeReport(Locale locale) {
        DependencyManagementRenderer r = new DependencyManagementRenderer(
                getSink(),
                locale,
                getI18N(locale),
                getLog(),
                getManagementDependencies(),
                repoSession,
                remoteProjectRepositories,
                repositorySystem,
                artifactHandlerManager,
                repoUtils,
                getLicenseMappings(),
                hasManagedDependencyWithoutVersion() ? resolveDependencies() : null);
        r.render();
    }

    /**
     * @deprecated use {@link #getOutputPath()} instead
     */
    @Override
    @Deprecated
    public String getOutputName() {
        return getOutputPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputPath() {
        return "dependency-management";
    }

    @Override
    protected String getI18Nsection() {
        return "dependency-management";
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private ManagementDependencies getManagementDependencies() {
        if (managementDependencies != null) {
            return managementDependencies;
        }

        if (project.getDependencyManagement() == null) {
            managementDependencies = new ManagementDependencies(null);
        } else {
            managementDependencies =
                    new ManagementDependencies(project.getDependencyManagement().getDependencies());
        }

        return managementDependencies;
    }

    /**
     * @return true if at least one managed dependency declares no version.
     */
    private boolean hasManagedDependencyWithoutVersion() {
        List<Dependency> managementDependencies = getManagementDependencies().getManagementDependencies();
        for (Dependency dependency : managementDependencies) {
            if (StringUtils.isBlank(dependency.getVersion())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the project's transitive dependencies exactly as a normal build (e.g. {@code mvn test}) would,
     * and flatten the result into a map keyed by {@code groupId:artifactId}. This is used to look up the
     * version that would apply to a managed dependency declaring no version of its own (MPIR-397): that
     * version can only come from the project's actual, mediated dependency graph.
     * <p>
     * {@link ProjectDependenciesResolver} is the same core component Maven's own lifecycle uses internally to
     * satisfy a Mojo's {@code requiresDependencyResolution}; calling it directly here means the resolution
     * does not depend on how this report happens to be invoked (a plain {@code mvn site}, a direct
     * {@code dependency-management} goal execution, an IDE, etc.).
     *
     * @return the resolved dependencies of the project, keyed by {@code groupId:artifactId}
     */
    private Map<String, Artifact> resolveDependencies() {
        Map<String, Artifact> resolvedArtifacts = new HashMap<>();

        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, repoSession);
        DependencyResolutionResult result;
        try {
            result = projectDependenciesResolver.resolve(request);
        } catch (DependencyResolutionException e) {
            getLog().warn("Unable to fully resolve project dependencies for the dependency-management report.", e);
            result = e.getResult();
            if (result == null) {
                return resolvedArtifacts;
            }
        }

        for (org.eclipse.aether.graph.Dependency dependency : result.getResolvedDependencies()) {
            Artifact artifact = org.apache.maven.RepositoryUtils.toArtifact(dependency.getArtifact());
            resolvedArtifacts.put(artifact.getGroupId() + ':' + artifact.getArtifactId(), artifact);
        }
        return resolvedArtifacts;
    }
}
