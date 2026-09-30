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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;

/**
 * Builds the {@link DependencyNode} trees of a project directly from Maven Resolver.
 *
 * @since 3.9.1
 */
public final class DependencyTreeBuilder {
    private DependencyTreeBuilder() {}

    /**
     * Builds the tree Maven resolves for the project, without downloading any artifact but the POMs.
     *
     * @param resolver the project dependencies resolver
     * @param project the project
     * @param session the repository session
     * @param filter filters the artifacts of the nodes below the root, may be <code>null</code>
     * @return the root node
     * @throws DependencyTreeException if the dependencies cannot be resolved
     */
    public static DependencyNode resolve(
            ProjectDependenciesResolver resolver,
            MavenProject project,
            RepositorySystemSession session,
            ArtifactFilter filter)
            throws DependencyTreeException {
        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
        request.setMavenProject(project);
        request.setRepositorySession(session);
        // only download the poms, not the artifacts
        DependencyFilter noArtifacts = (node, parents) -> false;
        request.setResolutionFilter(noArtifacts);

        DependencyResolutionResult result;
        try {
            result = resolver.resolve(request);
        } catch (DependencyResolutionException e) {
            throw new DependencyTreeException(
                    "Could not resolve following dependencies: " + e.getResult().getUnresolvedDependencies(), e);
        }

        return toNode(null, result.getDependencyGraph(), project.getArtifact(), filter);
    }

    /**
     * Collects the tree of the project including the nodes omitted for a conflict or a duplicate, which
     * {@link #resolve} leaves out.
     *
     * @param repositorySystem the repository system
     * @param project the project
     * @param session the repository session
     * @param filter filters the artifacts of the nodes below the root, may be <code>null</code>
     * @return the root node
     * @throws DependencyTreeException if the dependencies cannot be collected
     */
    public static DependencyNode collectVerbose(
            RepositorySystem repositorySystem,
            MavenProject project,
            RepositorySystemSession session,
            ArtifactFilter filter)
            throws DependencyTreeException {
        DefaultRepositorySystemSession verboseSession = new DefaultRepositorySystemSession(session);
        verboseSession.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
        verboseSession.setDependencyGraphTransformer(new ConflictResolver(
                new NearestVersionSelector(),
                new JavaScopeSelector(),
                new SimpleOptionalitySelector(),
                new JavaScopeDeriver()));
        verboseSession.setDependencySelector(new AndDependencySelector(
                new DirectScopeDependencySelector(JavaScopes.TEST),
                new DirectScopeDependencySelector(JavaScopes.PROVIDED),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()));

        Artifact projectArtifact = project.getArtifact();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(RepositoryUtils.toArtifact(projectArtifact));
        collectRequest.setRepositories(RepositoryUtils.toRepos(project.getRemoteArtifactRepositories()));

        ArtifactTypeRegistry stereotypes = verboseSession.getArtifactTypeRegistry();
        for (Dependency dependency : project.getDependencies()) {
            collectRequest.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
        }
        if (project.getDependencyManagement() != null) {
            for (Dependency dependency : project.getDependencyManagement().getDependencies()) {
                collectRequest.addManagedDependency(RepositoryUtils.toDependency(dependency, stereotypes));
            }
        }

        try {
            return toNode(
                    null,
                    repositorySystem
                            .collectDependencies(verboseSession, collectRequest)
                            .getRoot(),
                    projectArtifact,
                    filter);
        } catch (DependencyCollectionException e) {
            throw new DependencyTreeException("Could not collect dependencies: " + e.getResult(), e);
        }
    }

    private static DependencyNode toNode(
            DependencyNode parent,
            org.eclipse.aether.graph.DependencyNode node,
            Artifact artifact,
            ArtifactFilter filter) {
        DependencyNode current = new DependencyNode(parent, artifact, artifact.isOptional());

        List<DependencyNode> children = new ArrayList<>(node.getChildren().size());
        for (org.eclipse.aether.graph.DependencyNode child : node.getChildren()) {
            Artifact childArtifact = toArtifact(child.getDependency());
            if (filter == null || filter.include(childArtifact)) {
                children.add(toNode(current, child, childArtifact, filter));
            }
        }
        current.setChildren(children);

        return current;
    }

    private static Artifact toArtifact(org.eclipse.aether.graph.Dependency dependency) {
        Artifact artifact = RepositoryUtils.toArtifact(dependency.getArtifact());
        artifact.setScope(dependency.getScope());
        artifact.setOptional(dependency.isOptional());
        return artifact;
    }

    /**
     * Excludes a scope from the direct dependencies' own dependencies (depth 2 and deeper) only.
     */
    private static final class DirectScopeDependencySelector implements DependencySelector {
        private final String scope;

        private final int depth;

        DirectScopeDependencySelector(String scope) {
            this(scope, 0);
        }

        private DirectScopeDependencySelector(String scope, int depth) {
            this.scope = scope;
            this.depth = depth;
        }

        @Override
        public boolean selectDependency(org.eclipse.aether.graph.Dependency dependency) {
            return depth < 2 || !scope.equals(dependency.getScope());
        }

        @Override
        public DependencySelector deriveChildSelector(
                org.eclipse.aether.collection.DependencyCollectionContext context) {
            return depth >= 2 ? this : new DirectScopeDependencySelector(scope, depth + 1);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            DirectScopeDependencySelector other = (DirectScopeDependencySelector) obj;
            return depth == other.depth && scope.equals(other.scope);
        }

        @Override
        public int hashCode() {
            return 31 * (31 + depth) + scope.hashCode();
        }
    }
}
