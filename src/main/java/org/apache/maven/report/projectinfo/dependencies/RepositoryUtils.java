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
package org.apache.maven.report.projectinfo.dependencies;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Utility methods to play with repository.
 *
 * @version $Id$
 * @since 2.1
 */
@Named
@Singleton
public class RepositoryUtils {

    private final ProjectBuilder projectBuilder;

    private final RepositorySystem repositorySystem;

    private final Provider<MavenSession> sessionProvider;

    /**
     * @param projectBuilder {@link ProjectBuilder}
     */
    @Inject
    public RepositoryUtils(
            ProjectBuilder projectBuilder, RepositorySystem repositorySystem, Provider<MavenSession> sessionProvider) {
        this.projectBuilder = projectBuilder;
        this.repositorySystem = repositorySystem;
        this.sessionProvider = sessionProvider;
    }

    /**
     * @param artifact not null
     * @throws ArtifactResolutionException if any
     */
    public void resolve(Artifact artifact) throws ArtifactResolutionException {

        MavenSession session = sessionProvider.get();
        MavenProject project = session.getCurrentProject();

        ArtifactRequest request = new ArtifactRequest(
                org.apache.maven.RepositoryUtils.toArtifact(artifact), project.getRemoteProjectRepositories(), null);
        ArtifactResult result = repositorySystem.resolveArtifact(session.getRepositorySession(), request);

        artifact.setFile(result.getArtifact().getFile());
        artifact.setResolved(true);
    }

    /**
     * Get the <code>Maven project</code> from the repository depending the <code>Artifact</code> given.
     *
     * @param artifact an artifact
     * @return the Maven project for the given artifact
     * @throws ProjectBuildingException if any
     */
    public MavenProject getMavenProjectFromRepository(Artifact artifact) throws ProjectBuildingException {

        boolean allowStubModel = false;
        if (!"pom".equals(artifact.getType())) {
            allowStubModel = true;
        }

        MavenSession session = sessionProvider.get();

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(session.getCurrentProject().getRemoteArtifactRepositories());
        buildingRequest.setProcessPlugins(false);

        return projectBuilder.build(artifact, allowStubModel, buildingRequest).getProject();
    }
}
