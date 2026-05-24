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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.jar.JarAnalyzer;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 2.1
 */
public class Dependencies {
    private static final Logger LOG = LoggerFactory.getLogger(Dependencies.class);

    private final MavenProject project;

    private final DependencyNode dependencyNode;

    private final JarClassesAnalysis classesAnalyzer;

    /**
     * @since 2.1
     */
    private List<Artifact> projectDependencies;

    /**
     * @since 2.1
     */
    private List<Artifact> projectTransitiveDependencies;

    /**
     * @since 2.1
     */
    private List<Artifact> allDependencies;

    /**
     * @since 2.1
     */
    private Map<String, List<Artifact>> dependenciesByScope;

    /**
     * @since 2.1
     */
    private Map<String, List<Artifact>> transitiveDependenciesByScope;

    /**
     * @since 2.1
     */
    private Map<String, JarDataSummary> dependencyDetails;

    /**
     * @since 3.9.1
     */
    private File pluginCacheBaseDir;

    /**
     * @since 3.9.1
     */
    private ObjectWriter objectWriter;

    /**
     * @since 3.9.1
     */
    private ObjectReader objectReader;

    /**
     * Default constructor
     *
     * @param project the MavenProject.
     * @param dependencyTreeNode the DependencyNode.
     * @param classesAnalyzer the JarClassesAnalysis.
     * @param pluginCacheBaseDir the base dir for the cache file
     */
    public Dependencies(
            MavenProject project,
            DependencyNode dependencyTreeNode,
            JarClassesAnalysis classesAnalyzer,
            File pluginCacheBaseDir) {
        this.project = project;
        this.dependencyNode = dependencyTreeNode;
        this.classesAnalyzer = classesAnalyzer;
        this.dependencyDetails = new HashMap<>();
        this.pluginCacheBaseDir = pluginCacheBaseDir;

        ObjectMapper mapper = new ObjectMapper();
        objectReader = mapper.readerFor(JarDataSummary.class);
        objectWriter = mapper.writer();
    }

    /**
     * Getter for the project
     *
     * @return the project
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * @return <code>true</code> if getProjectDependencies() is not empty, <code>false</code> otherwise.
     */
    public boolean hasDependencies() {
        return (getProjectDependencies() != null) && (!getProjectDependencies().isEmpty());
    }

    /**
     * @return a list of <code>Artifact</code> from the project.
     */
    public List<Artifact> getProjectDependencies() {
        if (projectDependencies != null) {
            return projectDependencies;
        }

        projectDependencies = new ArrayList<>();
        for (DependencyNode dep : dependencyNode.getChildren()) {
            projectDependencies.add(dep.getArtifact());
        }

        return projectDependencies;
    }

    /**
     * @return a list of transitive <code>Artifact</code> from the project.
     */
    public List<Artifact> getTransitiveDependencies() {
        if (projectTransitiveDependencies != null) {
            return projectTransitiveDependencies;
        }

        projectTransitiveDependencies = new ArrayList<>(getAllDependencies());
        projectTransitiveDependencies.removeAll(getProjectDependencies());

        return projectTransitiveDependencies;
    }

    /**
     * @return a list of included <code>Artifact</code> returned by the dependency tree.
     */
    public List<Artifact> getAllDependencies() {
        if (allDependencies != null) {
            return allDependencies;
        }

        allDependencies = new ArrayList<>();

        addAllChildrenDependencies(dependencyNode);

        return allDependencies;
    }

    /**
     * @param isTransitively <code>true</code> to return transitive dependencies, <code>false</code> otherwise.
     * @return a map with supported scopes as key and a list of <code>Artifact</code> as values.
     * @see Artifact#SCOPE_COMPILE
     * @see Artifact#SCOPE_PROVIDED
     * @see Artifact#SCOPE_RUNTIME
     * @see Artifact#SCOPE_SYSTEM
     * @see Artifact#SCOPE_TEST
     */
    public Map<String, List<Artifact>> getDependenciesByScope(boolean isTransitively) {
        if (isTransitively) {
            if (transitiveDependenciesByScope != null) {
                return transitiveDependenciesByScope;
            }

            transitiveDependenciesByScope = new HashMap<>();
            for (Artifact artifact : getTransitiveDependencies()) {
                List<Artifact> multiValue = transitiveDependenciesByScope.get(artifact.getScope());
                if (multiValue == null) {
                    multiValue = new ArrayList<>();
                }

                if (!multiValue.contains(artifact)) {
                    multiValue.add(artifact);
                }
                transitiveDependenciesByScope.put(artifact.getScope(), multiValue);
            }

            return transitiveDependenciesByScope;
        }

        if (dependenciesByScope != null) {
            return dependenciesByScope;
        }

        dependenciesByScope = new HashMap<>();
        for (Artifact artifact : getProjectDependencies()) {
            List<Artifact> multiValue = dependenciesByScope.get(artifact.getScope());
            if (multiValue == null) {
                multiValue = new ArrayList<>();
            }

            if (!multiValue.contains(artifact)) {
                multiValue.add(artifact);
            }
            dependenciesByScope.put(artifact.getScope(), multiValue);
        }

        return dependenciesByScope;
    }

    /**
     * Get details on the content of the JAR file.
     *
     * @param artifact the artifact.
     * @return the jardata object from the artifact
     * @throws IOException if any
     */
    public JarDataSummary getJarDependencyDetails(Artifact artifact) throws IOException {
        JarDataSummary jarDataSummary = dependencyDetails.get(artifact.getId());
        if (jarDataSummary != null) {
            return jarDataSummary;
        }

        File file = getFile(artifact);

        if (file.isDirectory()) {
            jarDataSummary = JarDataSummary.DIRECTORY_JAR_DATA_SUMMARY;
        } else {
            jarDataSummary = extractJarDataFromDependency(artifact, file);
        }

        dependencyDetails.put(artifact.getId(), jarDataSummary);

        return jarDataSummary;
    }

    private JarDataSummary extractJarDataFromDependency(Artifact artifact, File file) throws IOException {
        Path cachedArtifactDir = getCacheDirectory(pluginCacheBaseDir, artifact);
        Path cacheFile = cachedArtifactDir.resolve("jar-data.json");

        BasicFileAttributes fileAttr = null;

        if (Files.exists(cacheFile)) {
            try {
                JarDataSummary jarDataSummary = objectReader.readValue(cacheFile.toFile());

                // cheap file change check
                fileAttr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                if (jarDataSummary.getFsize() == fileAttr.size()
                        && jarDataSummary.getTs() == fileAttr.lastModifiedTime().toMillis()) {
                    LOG.debug("JarDataSummary cached for: {}", artifact);
                    return jarDataSummary;
                }
            } catch (IOException ioe) {
                LOG.warn("Loading JarDataSummary from cache failed: {}", artifact, ioe);
            }
        }

        JarAnalyzer jarAnalyzer = new JarAnalyzer(file);

        try {
            classesAnalyzer.analyze(jarAnalyzer);
        } finally {
            jarAnalyzer.closeQuietly();
        }

        JarData jarData = jarAnalyzer.getJarData();
        JarDataSummary jarDataSummary = JarDataSummary.fromJarData(jarData);
        if (fileAttr == null) {
            fileAttr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        }
        jarDataSummary.setFileAttributes(fileAttr);

        // Save the result to our cache for next time
        try {
            Files.createDirectories(cachedArtifactDir);
            objectWriter.writeValue(cacheFile.toFile(), jarDataSummary);
        } catch (IOException ioe) {
            LOG.warn("Saving JarDataSummary to cache failed: {}", artifact, ioe);
        }
        LOG.debug("JarDataSummary analyzed for: {}", artifact);
        return jarDataSummary;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Recursive method to get all dependencies from a given <code>dependencyNode</code>
     *
     * @param dependencyNode not null
     */
    private void addAllChildrenDependencies(DependencyNode dependencyNode) {
        for (DependencyNode subdependencyNode : dependencyNode.getChildren()) {
            Artifact artifact = subdependencyNode.getArtifact();

            if (artifact.getGroupId().equals(project.getGroupId())
                    && artifact.getArtifactId().equals(project.getArtifactId())
                    && artifact.getVersion().equals(project.getVersion())) {
                continue;
            }

            if (!allDependencies.contains(artifact)) {
                allDependencies.add(artifact);
            }

            addAllChildrenDependencies(subdependencyNode);
        }
    }

    /**
     * get the artifact's file, with detection of target/classes directory with already packaged jar available.
     *
     * @param artifact the artifact to retrieve the physical file
     * @return the physical file representing the given artifact
     */
    public File getFile(Artifact artifact) {
        File file = artifact.getFile();

        if (file.isDirectory()) {
            // MPIR-322: if target/classes directory, try
            // target/artifactId-version[-classifier].jar
            String filename = artifact.getArtifactId() + '-' + artifact.getVersion();
            if (StringUtils.isNotEmpty(artifact.getClassifier())) {
                filename += '-' + artifact.getClassifier();
            }
            filename += '.' + artifact.getType();

            File jar = new File(file, "../" + filename);

            if (jar.exists()) {
                return jar;
            }
        }

        return file;
    }

    /**
     * Generates a cache directory following the GAV + Classifier structure. Path:
     * root/.cache/<plugin>/groupId/artifactId/version/[classifier]
     */
    private Path getCacheDirectory(File root, Artifact artifact) {
        // 1. Convert dots to folder separators for the GroupId
        String groupPath = artifact.getGroupId().replace('.', File.separatorChar);

        // 2. Build the base path: groupId / artifactId / version
        Path path = Paths.get(root.getAbsolutePath(), groupPath, artifact.getArtifactId(), artifact.getVersion());

        // 3. Handle the Classifier if it exists
        // Most artifacts don't have one (null or empty string)
        if (artifact.hasClassifier()) {
            path = path.resolve(artifact.getClassifier());
        }

        return path;
    }
}
