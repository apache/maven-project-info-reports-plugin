package org.apache.maven.report.projectinfo;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Plugin Management report.
 *
 * @author Nick Stolwijk
 * @since 2.1
 */
@Mojo( name = "plugin-management", requiresDependencyResolution = ResolutionScope.TEST )
public class PluginManagementReport
    extends AbstractProjectInfoReport
{
    
    /**
     * Specify the excluded plugins. This can be a list of artifacts in the format
     * groupId[:artifactId[:type[:version]]]. <br>
     * Plugins matching any exclude will not be present in the report.
     * 
     * @since 3.0.1
     */
    @Parameter
    private List<String> pluginManagementExcludes = null;
    
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void executeReport( Locale locale )
    {
        PluginManagementRenderer r =
            new PluginManagementRenderer( getLog(), getSink(), locale, getI18N( locale ),
                                          project.getPluginManagement().getPlugins(), project, projectBuilder,
                                          repositorySystem, getSession().getProjectBuildingRequest(),
                                          pluginManagementExcludes );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "plugin-management";
    }

    @Override
    protected String getI18Nsection()
    {
        return "plugin-management";
    }

    @Override
    public boolean canGenerateReport()
    {
          boolean result = super.canGenerateReport();
          if ( result && skipEmptyReport )
          {
              result = getProject().getPluginManagement() != null
                      && !isEmpty( project.getPluginManagement().getPlugins() );
          }

          return result;
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     *
     * @author Nick Stolwijk
     */
    protected static class PluginManagementRenderer
        extends AbstractProjectInfoRenderer
    {

        private final Log log;

        private final List<Plugin> pluginManagement;

        private final MavenProject project;

        private final ProjectBuilder projectBuilder;

        private final RepositorySystem repositorySystem;

        private final ProjectBuildingRequest buildingRequest;
        
        private final PatternExcludesArtifactFilter patternExcludesArtifactFilter;

        /**
         * @param log {@link #log}
         * @param sink {@link Sink}
         * @param locale {@link Locale}
         * @param i18n {@link I18N}
         * @param plugins {@link Plugin}
         * @param project {@link MavenProject}
         * @param projectBuilder {@link ProjectBuilder}
         * @param repositorySystem {@link RepositorySystem}
         * @param buildingRequest {@link ProjectBuildingRequest}
         * @param excludes the list of plugins to be excluded from the report
         */
        public PluginManagementRenderer( Log log, Sink sink, Locale locale, I18N i18n, List<Plugin> plugins,
                                         MavenProject project, ProjectBuilder projectBuilder,
                                         RepositorySystem repositorySystem, ProjectBuildingRequest buildingRequest,
                                         List<String> excludes )
        {
            super( sink, i18n, locale );

            this.log = log;

            this.pluginManagement = plugins;

            this.project = project;

            this.projectBuilder = projectBuilder;

            this.repositorySystem = repositorySystem;

            this.buildingRequest = buildingRequest;

            this.patternExcludesArtifactFilter = new PatternExcludesArtifactFilter( excludes );
        }

        @Override
        protected String getI18Nsection()
        {
            return "plugin-management";
        }

        @Override
        public void renderBody()
        {
            PluginManagement projectPluginManagement = project.getPluginManagement();

            if ( projectPluginManagement == null || projectPluginManagement.getPlugins() == null
                || projectPluginManagement.getPlugins().isEmpty() )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "nolist" ) );

                endSection();

                return;
            }

            // === Section: Project PluginManagement.
            renderSectionPluginManagement();
        }

        private void renderSectionPluginManagement()
        {
            String[] tableHeader = getPluginTableHeader();

            startSection( getTitle() );

            // can't use straight artifact comparison because we want optional last
            Collections.sort( pluginManagement, getPluginComparator() );

            startTable();
            tableHeader( tableHeader );

            ProjectBuildingRequest buildRequest = new DefaultProjectBuildingRequest( buildingRequest );
            buildRequest.setRemoteRepositories( project.getPluginArtifactRepositories() );
            
            for ( Plugin plugin : pluginManagement )
            {
                VersionRange versionRange;
                if ( StringUtils.isEmpty( plugin.getVersion() ) )
                {
                    versionRange = VersionRange.createFromVersion( Artifact.RELEASE_VERSION );
                }
                else
                {
                    versionRange = VersionRange.createFromVersion( plugin.getVersion() );
                }

                Artifact pluginArtifact = repositorySystem.createProjectArtifact( plugin.getGroupId(), plugin
                    .getArtifactId(), versionRange.toString() );

                if ( patternExcludesArtifactFilter.include( pluginArtifact ) )
                {
                    try
                    {
                        MavenProject pluginProject =
                            projectBuilder.build( pluginArtifact, buildingRequest ).getProject();

                        tableRow( getPluginRow( pluginProject.getGroupId(), pluginProject.getArtifactId(),
                                                pluginProject.getVersion(), pluginProject.getUrl() ) );
                    }
                    catch ( ProjectBuildingException e )
                    {
                        log.info( "Could not build project for: " + plugin.getArtifactId() + ":" + e.getMessage(), e );
                        tableRow( getPluginRow( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(),
                                                null ) );
                    }
                }
                else
                {
                    log.debug( "Excluding plugin " + pluginArtifact.getId() + " from report" );
                }
            }
            endTable();

            endSection();
        }

        // ----------------------------------------------------------------------
        // Private methods
        // ----------------------------------------------------------------------

        private String[] getPluginTableHeader()
        {
            // reused key...
            String groupId = getI18nString( "dependency-management", "column.groupId" );
            String artifactId = getI18nString( "dependency-management", "column.artifactId" );
            String version = getI18nString( "dependency-management", "column.version" );
            return new String[] { groupId, artifactId, version };
        }

        private String[] getPluginRow( String groupId, String artifactId, String version, String link )
        {
            artifactId = ProjectInfoReportUtils.getArtifactIdCell( artifactId, link );
            return new String[] { groupId, artifactId, version };
        }

        private Comparator<Plugin> getPluginComparator()
        {
            return new Comparator<Plugin>()
            {
                /** {@inheritDoc} */
                public int compare( Plugin a1, Plugin a2 )
                {
                    int result = a1.getGroupId().compareTo( a2.getGroupId() );
                    if ( result == 0 )
                    {
                        result = a1.getArtifactId().compareTo( a2.getArtifactId() );
                    }
                    return result;
                }
            };
        }

    }
}
