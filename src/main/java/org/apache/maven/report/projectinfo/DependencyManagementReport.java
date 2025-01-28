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

import java.util.Locale;

import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependencyManagementRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.i18n.I18N;

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

    /**
     * Artifact metadata source component.
     *
     * @since 2.4
     */
    protected final ArtifactMetadataSource artifactMetadataSource;

    private final RepositoryUtils repoUtils;

    @Inject
    protected DependencyManagementReport(
            RepositorySystem repositorySystem,
            I18N i18n,
            ProjectBuilder projectBuilder,
            ArtifactMetadataSource artifactMetadataSource,
            RepositoryUtils repoUtils) {
        super(repositorySystem, i18n, projectBuilder);
        this.artifactMetadataSource = artifactMetadataSource;
        this.repoUtils = repoUtils;
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
        ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest(getSession().getProjectBuildingRequest());
        buildingRequest.setLocalRepository(getSession().getLocalRepository());
        buildingRequest.setRemoteRepositories(remoteRepositories);
        buildingRequest.setPluginArtifactRepositories(pluginRepositories);
        buildingRequest.setProcessPlugins(false);

        DependencyManagementRenderer r = new DependencyManagementRenderer(
                getSink(),
                locale,
                getI18N(locale),
                getLog(),
                getManagementDependencies(),
                artifactMetadataSource,
                repositorySystem,
                buildingRequest,
                repoUtils,
                getLicenseMappings());
        r.render();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName() {
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
}
