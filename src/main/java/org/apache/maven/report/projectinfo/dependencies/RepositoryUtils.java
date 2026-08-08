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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
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
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

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

    private final ArtifactHandlerManager artifactHandlerManager;

    private final RepositorySystem repositorySystem;

    private final Provider<MavenSession> sessionProvider;

    /**
     * @param projectBuilder {@link ProjectBuilder}
     * @param artifactHandlerManager {@link ArtifactHandlerManager}
     * @param repositorySystem {@link RepositorySystem}
     * @param sessionProvider the current {@link MavenSession}
     */
    @Inject
    public RepositoryUtils(
            ProjectBuilder projectBuilder,
            ArtifactHandlerManager artifactHandlerManager,
            RepositorySystem repositorySystem,
            Provider<MavenSession> sessionProvider) {
        this.projectBuilder = projectBuilder;
        this.artifactHandlerManager = artifactHandlerManager;
        this.repositorySystem = repositorySystem;
        this.sessionProvider = sessionProvider;
    }

    /**
     * Create an artifact of type <code>pom</code> for the given coordinates.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version, may be a version range
     * @return the artifact, never {@code null}
     */
    public Artifact createProjectArtifact(String groupId, String artifactId, String version) {
        return createArtifact(groupId, artifactId, version, null, "pom");
    }

    /**
     * Create an artifact for the given coordinates.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version, may be a version range
     * @param scope the dependency scope, may be {@code null}
     * @param type the artifact type
     * @return the artifact, never {@code null}
     */
    public Artifact createArtifact(String groupId, String artifactId, String version, String scope, String type) {
        return new DefaultArtifact(
                groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                scope,
                type,
                null,
                artifactHandlerManager.getArtifactHandler(type));
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
     * Retrieve the versions available in the given remote repositories for the artifact, whose version may be a
     * version range. Only the versions matching that range are returned, sorted in ascending order.
     *
     * @param artifact the artifact, its version may be a version range
     * @param remoteRepositories the remote repositories to look the versions up in
     * @return the matching versions, possibly empty, never {@code null}
     * @throws VersionRangeResolutionException if the version range could not be resolved at all
     */
    public List<ArtifactVersion> getAvailableVersions(Artifact artifact, List<ArtifactRepository> remoteRepositories)
            throws VersionRangeResolutionException {

        MavenSession session = sessionProvider.get();

        VersionRangeRequest request = new VersionRangeRequest(
                org.apache.maven.RepositoryUtils.toArtifact(artifact),
                org.apache.maven.RepositoryUtils.toRepos(remoteRepositories),
                null);
        VersionRangeResult result = repositorySystem.resolveVersionRange(session.getRepositorySession(), request);

        List<ArtifactVersion> versions = new ArrayList<>(result.getVersions().size());
        for (Version version : result.getVersions()) {
            versions.add(new DefaultArtifactVersion(version.toString()));
        }
        return versions;
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
