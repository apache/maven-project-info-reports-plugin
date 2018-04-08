package org.apache.maven.report.projectinfo.dependencies;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utilities methods to play with repository
 *
 * @version $Id$
 * @since 2.1
 */
public class RepositoryUtils
{
    private final Log log;

    private final MavenProjectBuilder mavenProjectBuilder;

    private final ArtifactFactory factory;

    private final List<ArtifactRepository> remoteRepositories;

    private final List<ArtifactRepository> pluginRepositories;

    private final ArtifactResolver resolver;

    private final ArtifactRepository localRepository;

    /**
     * @param log {@link Log}
     * @param mavenProjectBuilder {@link MavenProjectBuilder}
     * @param factory {@link ArtifactFactory}
     * @param resolver {@link ArtifactResolver}
     * @param remoteRepositories {@link ArtifactRepository}
     * @param pluginRepositories {@link ArtifactRepository}
     * @param localRepository {@link ArtifactRepository}
     * @param repositoryMetadataManager {@link RepositoryMetadataManager}
     */
    public RepositoryUtils( Log log, MavenProjectBuilder mavenProjectBuilder, ArtifactFactory factory,
                            ArtifactResolver resolver, List<ArtifactRepository> remoteRepositories,
                            List<ArtifactRepository> pluginRepositories, ArtifactRepository localRepository,
                            RepositoryMetadataManager repositoryMetadataManager )
    {
        this.log = log;
        this.mavenProjectBuilder = mavenProjectBuilder;
        this.factory = factory;
        this.resolver = resolver;
        this.remoteRepositories = remoteRepositories;
        this.pluginRepositories = pluginRepositories;
        this.localRepository = localRepository;
    }

    /**
     * @return localrepo
     */
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    /**
     * @return remote artifact repo
     */
    public List<ArtifactRepository> getRemoteArtifactRepositories()
    {
        return remoteRepositories;
    }

    /**
     * @return plugin artifact repo
     */
    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        return pluginRepositories;
    }

    /**
     * @param artifact not null
     * @throws ArtifactResolutionException if any
     * @throws ArtifactNotFoundException if any
     * @see ArtifactResolver#resolve(Artifact, List, ArtifactRepository)
     */
    public void resolve( Artifact artifact )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        List<ArtifactRepository> repos =
            new ArrayList<ArtifactRepository>( pluginRepositories.size() + remoteRepositories.size() );
        repos.addAll( pluginRepositories );
        repos.addAll( remoteRepositories );

        resolver.resolve( artifact, repos, localRepository );
    }

    /**
     * Get the <code>Maven project</code> from the repository depending the <code>Artifact</code> given.
     *
     * @param artifact an artifact
     * @return the Maven project for the given artifact
     * @throws ProjectBuildingException if any
     */
    public MavenProject getMavenProjectFromRepository( Artifact artifact )
        throws ProjectBuildingException
    {
        Artifact projectArtifact = artifact;

        boolean allowStubModel = false;
        if ( !"pom".equals( artifact.getType() ) )
        {
            projectArtifact = factory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                             artifact.getVersion(), artifact.getScope() );
            allowStubModel = true;
        }

        // TODO: we should use the MavenMetadataSource instead
        return mavenProjectBuilder.buildFromRepository( projectArtifact, remoteRepositories, localRepository,
                                                        allowStubModel );
    }

    /**
     * @param artifact not null
     * @param repo not null
     * @return the artifact url in the given repo for the given artifact. If it is a snapshot artifact, the version
     * will be the timestamp and the build number from the metadata. Could return null if the repo is blacklisted.
     */
    public String getDependencyUrlFromRepository( Artifact artifact, ArtifactRepository repo )
    {
        if ( repo.isBlacklisted() )
        {
            return null;
        }

        Artifact copyArtifact = ArtifactUtils.copyArtifact( artifact );
        // Try to get the last artifact repo name depending the snapshot version
        if ( ( artifact.isSnapshot() && repo.getSnapshots().isEnabled() ) )
        {
            if ( artifact.getBaseVersion().equals( artifact.getVersion() ) )
            {
                // Try to resolve it if not already done
                if ( artifact.getMetadataList() == null || artifact.getMetadataList().isEmpty() )
                {
                    try
                    {
                        resolve( artifact );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " could not be resolved." );
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " was not found." );
                    }
                }

                for ( ArtifactMetadata m : artifact.getMetadataList() )
                {
                    if ( m instanceof SnapshotArtifactRepositoryMetadata )
                    {
                        SnapshotArtifactRepositoryMetadata snapshotMetadata = (SnapshotArtifactRepositoryMetadata) m;

                        Metadata metadata = snapshotMetadata.getMetadata();
                        Versioning versioning = metadata.getVersioning();
                        if ( versioning == null || versioning.getSnapshot() == null
                            || versioning.getSnapshot().isLocalCopy()
                            || versioning.getSnapshot().getTimestamp() == null )
                        {
                            continue;
                        }

                        // create the version according SnapshotTransformation
                        String version =
                            StringUtils.replace( copyArtifact.getVersion(), Artifact.SNAPSHOT_VERSION,
                                                 versioning.getSnapshot().getTimestamp() )
                                + "-" + versioning.getSnapshot().getBuildNumber();
                        copyArtifact.setVersion( version );
                    }
                }
            }
        }

        return repo.getUrl() + "/" + repo.pathOf( copyArtifact );
    }
}
