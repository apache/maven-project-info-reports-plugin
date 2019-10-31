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

import java.util.Locale;

import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependencyManagementRenderer;

/**
 * Generates the Project Dependency Management report.
 *
 * @author Nick Stolwijk
 * @since 2.1
 */
@Mojo( name = "dependency-management", requiresDependencyResolution = ResolutionScope.TEST )
public class DependencyManagementReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Artifact metadata source component.
     *
     * @since 2.4
     */
    @Component
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * Repository metadata component.
     *
     * @since 2.3
     */
    @Component
    private RepositoryMetadataManager repositoryMetadataManager;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Lazy instantiation for management dependencies.
     */
    private ManagementDependencies managementDependencies;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            result = getManagementDependencies().hasDependencies();
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        ProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest( getSession().getProjectBuildingRequest() );
        buildingRequest.setLocalRepository( localRepository );
        buildingRequest.setRemoteRepositories( remoteRepositories );
        buildingRequest.setPluginArtifactRepositories( pluginRepositories );
        
        RepositoryUtils repoUtils =
            new RepositoryUtils( getLog(), projectBuilder, repositorySystem, resolver,
                                 project.getRemoteArtifactRepositories(), project.getPluginArtifactRepositories(),
                                 buildingRequest, repositoryMetadataManager );

        DependencyManagementRenderer r =
            new DependencyManagementRenderer( getSink(), locale, getI18N( locale ), getLog(),
                                              getManagementDependencies(), artifactMetadataSource, repositorySystem,
                                              projectBuilder, buildingRequest, repoUtils );
        r.render();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "dependency-management";
    }

    @Override
    protected String getI18Nsection()
    {
        return "dependency-management";
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private ManagementDependencies getManagementDependencies()
    {
        if ( managementDependencies != null )
        {
            return managementDependencies;
        }

        if ( project.getDependencyManagement() == null )
        {
            managementDependencies = new ManagementDependencies( null );
        }
        else
        {
            managementDependencies = new ManagementDependencies( project.getDependencyManagement().getDependencies() );
        }

        return managementDependencies;
    }
}
