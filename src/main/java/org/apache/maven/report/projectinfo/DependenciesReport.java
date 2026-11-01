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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.DependencyNode;
import org.apache.maven.report.projectinfo.dependencies.DependencyTreeBuilder;
import org.apache.maven.report.projectinfo.dependencies.DependencyTreeException;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependenciesRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;
import org.codehaus.plexus.i18n.I18N;
import org.eclipse.aether.RepositorySystem;

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @since 2.0
 */
@Mojo(name = "dependencies", requiresDependencyResolution = ResolutionScope.TEST)
public class DependenciesReport extends AbstractProjectInfoReport {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Display file details for each dependency, such as: file size, number of
     * classes, number of packages etc.
     *
     * @since 2.1
     */
    @Parameter(property = "dependency.details.enabled", defaultValue = "true")
    private boolean dependencyDetailsEnabled;

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Project dependencies resolver component.
     *
     * @since 3.9.1
     */
    private final ProjectDependenciesResolver dependenciesResolver;

    /**
     * Jar classes analyzer component.
     *
     * @since 2.1
     */
    private final JarClassesAnalysis classesAnalyzer;

    private final RepositoryUtils repoUtils;

    @Inject
    protected DependenciesReport(
            RepositorySystem repositorySystem,
            I18N i18n,
            ProjectBuilder projectBuilder,
            ProjectDependenciesResolver dependenciesResolver,
            JarClassesAnalysis classesAnalyzer,
            RepositoryUtils repoUtils) {
        super(repositorySystem, i18n, projectBuilder);
        this.dependenciesResolver = dependenciesResolver;
        this.classesAnalyzer = classesAnalyzer;
        this.repoUtils = repoUtils;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        boolean result = super.canGenerateReport();
        if (result && skipEmptyReport) {
            // This seems to be a bit too much but the DependenciesRenderer applies the same logic
            DependencyNode dependencyNode = resolveProject();
            Dependencies dependencies = new Dependencies(project, dependencyNode, classesAnalyzer);
            result = dependencies.hasDependencies();
        }

        return result;
    }

    @Override
    public void executeReport(Locale locale) {
        DependencyNode dependencyNode = resolveProject();

        Dependencies dependencies = new Dependencies(project, dependencyNode, classesAnalyzer);

        DependenciesReportConfiguration config = new DependenciesReportConfiguration(dependencyDetailsEnabled);

        DependenciesRenderer r = new DependenciesRenderer(
                getSink(),
                locale,
                getI18N(locale),
                getLog(),
                dependencies,
                dependencyNode,
                config,
                repoUtils,
                getLicenseMappings());
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
        return "dependencies";
    }

    @Override
    protected String getI18Nsection() {
        return "dependencies";
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @return resolve the dependency tree
     */
    private DependencyNode resolveProject() {
        try {
            ArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_TEST);
            return DependencyTreeBuilder.resolve(
                    dependenciesResolver, project, getSession().getRepositorySession(), artifactFilter);
        } catch (DependencyTreeException e) {
            getLog().error("Unable to build dependency tree.", e);
            return null;
        }
    }
}
