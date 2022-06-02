package org.apache.maven.report.projectinfo.dependencies.renderer;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.LicenseMapping;
import org.apache.maven.report.projectinfo.ProjectInfoReportUtils;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Nick Stolwijk
 * @since 2.1
 */
public class DependencyManagementRenderer
    extends AbstractProjectInfoRenderer
{
    private final ManagementDependencies dependencies;

    private final Log log;

    private final ArtifactMetadataSource artifactMetadataSource;

    private final RepositorySystem repositorySystem;

    private final ProjectBuilder projectBuilder;

    private final ProjectBuildingRequest buildingRequest;

    private final RepositoryUtils repoUtils;

    private final Map<String, String> licenseMappings;

    /**
     * Default constructor
     *
     * @param sink {@link Sink}
     * @param locale {@link Locale}
     * @param i18n {@link I18N}
     * @param log {@link Log}
     * @param dependencies {@link ManagementDependencies}
     * @param artifactMetadataSource {@link ArtifactMetadataSource}
     * @param repositorySystem {@link RepositorySystem}
     * @param projectBuilder {@link ProjectBuilder}
     * @param buildingRequest {@link ProjectBuildingRequest}
     * @param repoUtils {@link RepositoryUtils}
     * @param licenseMappings {@link LicenseMapping}
     */
    public DependencyManagementRenderer( Sink sink, Locale locale, I18N i18n, Log log,
                                         ManagementDependencies dependencies,
                                         ArtifactMetadataSource artifactMetadataSource,
                                         RepositorySystem repositorySystem, ProjectBuilder projectBuilder,
                                         ProjectBuildingRequest buildingRequest, RepositoryUtils repoUtils,
                                         Map<String, String> licenseMappings )
    {
        super( sink, i18n, locale );

        this.log = log;
        this.dependencies = dependencies;
        this.artifactMetadataSource = artifactMetadataSource;
        this.repositorySystem = repositorySystem;
        this.projectBuilder = projectBuilder;
        this.buildingRequest = buildingRequest;
        this.repoUtils = repoUtils;
        this.licenseMappings = licenseMappings;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    protected String getI18Nsection()
    {
        return "dependency-management";
    }

    @Override
    public void renderBody()
    {
        // Dependencies report

        if ( !dependencies.hasDependencies() )
        {
            startSection( getTitle() );

            paragraph( getI18nString( "nolist" ) );

            endSection();

            return;
        }

        // === Section: Project Dependencies.
        renderSectionProjectDependencies();
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private void renderSectionProjectDependencies()
    {
        startSection( getTitle() );

        // collect dependencies by scope
        Map<String, List<Dependency>> dependenciesByScope = dependencies.getManagementDependenciesByScope();

        renderDependenciesForAllScopes( dependenciesByScope );

        endSection();
    }

    private void renderDependenciesForAllScopes( Map<String, List<Dependency>> dependenciesByScope )
    {
        renderDependenciesForScope( Artifact.SCOPE_COMPILE, dependenciesByScope.get( Artifact.SCOPE_COMPILE ) );
        renderDependenciesForScope( Artifact.SCOPE_RUNTIME, dependenciesByScope.get( Artifact.SCOPE_RUNTIME ) );
        renderDependenciesForScope( Artifact.SCOPE_TEST, dependenciesByScope.get( Artifact.SCOPE_TEST ) );
        renderDependenciesForScope( Artifact.SCOPE_PROVIDED, dependenciesByScope.get( Artifact.SCOPE_PROVIDED ) );
        renderDependenciesForScope( Artifact.SCOPE_SYSTEM, dependenciesByScope.get( Artifact.SCOPE_SYSTEM ) );
    }

    private String[] getDependencyTableHeader( boolean hasClassifier )
    {
        String groupId = getI18nString( "column.groupId" );
        String artifactId = getI18nString( "column.artifactId" );
        String version = getI18nString( "column.version" );
        String classifier = getI18nString( "column.classifier" );
        String type = getI18nString( "column.type" );
        String license = getI18nString( "column.license" );

        if ( hasClassifier )
        {
            return new String[] { groupId, artifactId, version, classifier, type, license };
        }

        return new String[] { groupId, artifactId, version, type, license };
    }

    private void renderDependenciesForScope( String scope, List<Dependency> artifacts )
    {
        if ( artifacts != null )
        {
            // can't use straight artifact comparison because we want optional last
            Collections.sort( artifacts, getDependencyComparator() );

            startSection( scope );

            paragraph( getI18nString( "intro." + scope ) );
            startTable();

            boolean hasClassifier = false;
            for ( Dependency dependency : artifacts )
            {
                if ( StringUtils.isNotEmpty( dependency.getClassifier() ) )
                {
                    hasClassifier = true;
                    break;
                }
            }

            String[] tableHeader = getDependencyTableHeader( hasClassifier );
            tableHeader( tableHeader );

            for ( Dependency dependency : artifacts )
            {
                tableRow( getDependencyRow( dependency, hasClassifier ) );
            }
            endTable();

            endSection();
        }
    }

    @SuppressWarnings( "unchecked" )
    private String[] getDependencyRow( Dependency dependency, boolean hasClassifier )
    {
        Artifact artifact =
            repositorySystem.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                             dependency.getVersion(), dependency.getScope(), dependency.getType() );

        StringBuilder licensesBuffer = new StringBuilder();
        String url = null;
        try
        {
            VersionRange range = VersionRange.createFromVersionSpec( dependency.getVersion() );

            if ( range.getRecommendedVersion() == null )
            {
                // MPIR-216: no direct version but version range: need to choose one precise version
                log.debug( "Resolving range for DependencyManagement on " + artifact.getId() );

                List<ArtifactVersion> versions =
                    artifactMetadataSource.retrieveAvailableVersions( artifact, buildingRequest.getLocalRepository(),
                                                                      buildingRequest.getRemoteRepositories() );

                // only use versions from range
                for ( Iterator<ArtifactVersion> iter = versions.iterator(); iter.hasNext(); )
                {
                    if ( ! range.containsVersion( iter.next() ) )
                    {
                        iter.remove();
                    }
                }

                // select latest, assuming pom information will be the most accurate
                if ( !versions.isEmpty() )
                {
                    ArtifactVersion maxArtifactVersion = Collections.max( versions );

                    artifact.setVersion( maxArtifactVersion.toString() );
                    log.debug( "DependencyManagement resolved: " + artifact.getId() );
                }
            }

            url = ProjectInfoReportUtils.getArtifactUrl( repositorySystem, artifact, projectBuilder, buildingRequest );

            MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact );

            List<License> licenses = artifactProject.getLicenses();
            for ( License license : licenses )
            {
                String name = license.getName();
                if ( licenseMappings != null && licenseMappings.containsKey( name ) )
                {
                    name = licenseMappings.get( name );
                }
                String licenseCell = ProjectInfoReportUtils.getArtifactIdCell( name, license.getUrl() );
                if ( licensesBuffer.length() > 0 )
                {
                    licensesBuffer.append( ", " );
                }
                licensesBuffer.append( licenseCell );
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            log.warn( "Unable to parse version for " + artifact.getId(), e );
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            log.warn( "Unable to retrieve versions for " + artifact.getId() + " from repository.", e );
        }
        catch ( ProjectBuildingException e )
        {
            if ( log.isDebugEnabled() )
            {
                log.warn( "Unable to create Maven project for " + artifact.getId() + " from repository.", e );
            }
            else
            {
                log.warn( "Unable to create Maven project for " + artifact.getId() + " from repository." );
            }
        }

        String artifactIdCell = ProjectInfoReportUtils.getArtifactIdCell( artifact.getArtifactId(), url );

        if ( hasClassifier )
        {
            return new String[] { dependency.getGroupId(), artifactIdCell, dependency.getVersion(),
                dependency.getClassifier(), dependency.getType(), licensesBuffer.toString() };
        }

        return new String[] { dependency.getGroupId(), artifactIdCell, dependency.getVersion(),
            dependency.getType(), licensesBuffer.toString() };
    }

    private Comparator<Dependency> getDependencyComparator()
    {
        return new Comparator<Dependency>()
        {
            public int compare( Dependency a1, Dependency a2 )
            {
                int result = a1.getGroupId().compareTo( a2.getGroupId() );
                if ( result != 0 )
                {
                    return result;
                }

                result = a1.getArtifactId().compareTo( a2.getArtifactId() );
                if ( result != 0 )
                {
                    return result;
                }

                result = a1.getType().compareTo( a2.getType() );
                if ( result != 0 )
                {
                    return result;
                }

                if ( a1.getClassifier() == null )
                {
                    if ( a2.getClassifier() != null )
                    {
                        return 1;
                    }
                }
                else
                {
                    if ( a2.getClassifier() != null )
                    {
                        result = a1.getClassifier().compareTo( a2.getClassifier() );
                    }
                    else
                    {
                        return -1;
                    }
                }

                if ( result != 0 )
                {
                    return result;
                }

                // We don't consider the version range in the comparison, just the resolved version
                return a1.getVersion().compareTo( a2.getVersion() );
            }
        };
    }
}
