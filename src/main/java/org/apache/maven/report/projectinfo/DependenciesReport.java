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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependenciesRenderer;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @since 2.0
 */
@Mojo( name = "dependencies", requiresDependencyResolution = ResolutionScope.TEST )
public class DependenciesReport
    extends AbstractProjectInfoReport
{
    /**
     * Images resources dir
     */
    private static final String RESOURCES_DIR = "org/apache/maven/report/projectinfo/resources";

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Dependency graph builder component.
     *
     * @since 2.5
     */
    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * Jar classes analyzer component.
     *
     * @since 2.1
     */
    @Component
    private JarClassesAnalysis classesAnalyzer;

    /**
     * Repository metadata component.
     *
     * @since 2.1
     */
    @Component
    private RepositoryMetadataManager repositoryMetadataManager;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Display file details for each dependency, such as: file size, number of
     * classes, number of packages etc.
     *
     * @since 2.1
     */
    @Parameter( property = "dependency.details.enabled", defaultValue = "true" )
    private boolean dependencyDetailsEnabled;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            // This seems to be a bit too much but the DependenciesRenderer applies the same logic
            DependencyNode dependencyNode = resolveProject();
            Dependencies dependencies = new Dependencies( project, dependencyNode, classesAnalyzer );
            result = dependencies.hasDependencies();
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        try
        {
            copyResources( new File( getOutputDirectory() ) );
        }
        catch ( IOException e )
        {
            getLog().error( "Cannot copy ressources", e );
        }
        
        ProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest( getSession().getProjectBuildingRequest() );
        buildingRequest.setLocalRepository( localRepository );
        buildingRequest.setRemoteRepositories( remoteRepositories );

        RepositoryUtils repoUtils =
            new RepositoryUtils( getLog(), projectBuilder, repositorySystem, resolver,
                                 project.getRemoteArtifactRepositories(), project.getPluginArtifactRepositories(),
                                 buildingRequest, repositoryMetadataManager );

        DependencyNode dependencyNode = resolveProject();

        Dependencies dependencies = new Dependencies( project, dependencyNode, classesAnalyzer );

        DependenciesReportConfiguration config =
            new DependenciesReportConfiguration( dependencyDetailsEnabled );

        DependenciesRenderer r =
            new DependenciesRenderer( getSink(), locale, getI18N( locale ), getLog(), dependencies,
                                      dependencyNode, config, repoUtils, repositorySystem, projectBuilder,
                                      buildingRequest );
        r.render();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "dependencies";
    }

    @Override
    protected String getI18Nsection()
    {
        return "dependencies";
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @return resolve the dependency tree
     */
    private DependencyNode resolveProject()
    {
        try
        {
            ArtifactFilter artifactFilter = new ScopeArtifactFilter( Artifact.SCOPE_TEST );
            return dependencyGraphBuilder.buildDependencyGraph( project, artifactFilter );
        }
        catch ( DependencyGraphBuilderException e )
        {
            getLog().error( "Unable to build dependency tree.", e );
            return null;
        }
    }

    /**
     * @param outputDirectory the wanted output directory
     * @throws IOException if any
     */
    private void copyResources( File outputDirectory )
        throws IOException
    {
        InputStream resourceList = null;
        InputStream in = null;
        BufferedReader reader = null;
        OutputStream out = null;
        try
        {
            resourceList = getClass().getClassLoader().getResourceAsStream( RESOURCES_DIR + "/resources.txt" );

            if ( resourceList != null )
            {
                reader = new LineNumberReader( new InputStreamReader( resourceList, ReaderFactory.US_ASCII ) );

                for ( String line = reader.readLine(); line != null; line = reader.readLine() )
                {
                    in = getClass().getClassLoader().getResourceAsStream( RESOURCES_DIR + "/" + line );

                    if ( in == null )
                    {
                        throw new IOException( "The resource " + line + " doesn't exist." );
                    }

                    File outputFile = new File( outputDirectory, line );

                    if ( !outputFile.getParentFile().exists() )
                    {
                        outputFile.getParentFile().mkdirs();
                    }

                    out = new FileOutputStream( outputFile );
                    IOUtil.copy( in, out );
                    out.close();
                    out = null;
                    in.close();
                    in = null;
                }

                reader.close();
                reader = null;
            }
        }
        finally
        {
            IOUtil.close( out );
            IOUtil.close( reader );
            IOUtil.close( in );
            IOUtil.close( resourceList );
        }
    }
}
