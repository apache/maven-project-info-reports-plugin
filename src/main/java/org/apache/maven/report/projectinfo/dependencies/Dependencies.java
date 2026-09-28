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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.jar.JarAnalyzer;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClasses;
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
    private Map<String, JarData> dependencyDetails;

    /**
     * @since 3.9.1
     */
    private final Map<String, JarDataSummary> dependencySummaries = new HashMap<>();

    /**
     * @since 3.9.1
     */
    private final File cacheDirectory;

    /**
     * Constructor which does not cache the file details of the dependencies between builds.
     *
     * @param project the MavenProject.
     * @param dependencyTreeNode the DependencyNode.
     * @param classesAnalyzer the JarClassesAnalysis.
     * @deprecated use {@link #Dependencies(MavenProject, DependencyNode, JarClassesAnalysis, File)} instead
     */
    @Deprecated
    public Dependencies(MavenProject project, DependencyNode dependencyTreeNode, JarClassesAnalysis classesAnalyzer) {
        this(project, dependencyTreeNode, classesAnalyzer, null);
    }

    /**
     * Default constructor
     *
     * @param project the MavenProject.
     * @param dependencyTreeNode the DependencyNode.
     * @param classesAnalyzer the JarClassesAnalysis.
     * @param cacheDirectory the directory where the summary of the file details of each dependency is cached
     *            between builds, or <code>null</code> to not cache them.
     * @since 3.9.1
     */
    public Dependencies(
            MavenProject project,
            DependencyNode dependencyTreeNode,
            JarClassesAnalysis classesAnalyzer,
            File cacheDirectory) {
        this.project = project;
        this.dependencyNode = dependencyTreeNode;
        this.classesAnalyzer = classesAnalyzer;
        this.cacheDirectory = cacheDirectory;
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
     * @param artifact the artifact.
     * @return the jardata object from the artifact
     * @throws IOException if any
     */
    public JarData getJarDependencyDetails(Artifact artifact) throws IOException {
        if (dependencyDetails == null) {
            dependencyDetails = new HashMap<>();
        }

        JarData jarData = dependencyDetails.get(artifact.getId());
        if (jarData != null) {
            return jarData;
        }

        File file = getFile(artifact);

        if (file.isDirectory()) {
            jarData = new JarData(artifact.getFile(), null, new ArrayList<JarEntry>());

            jarData.setJarClasses(new JarClasses());
        } else {
            jarData = analyze(file);
        }

        dependencyDetails.put(artifact.getId(), jarData);

        return jarData;
    }

    /**
     * Get a summary of the details on the content of the JAR file: the values that are shown in the report. Unlike
     * {@link #getJarDependencyDetails(Artifact)}, the JAR file is not analyzed again if the summary was cached by a
     * previous build and the file has not changed since.
     *
     * @param artifact the artifact.
     * @return the summary of the JAR file details
     * @throws IOException if the JAR file cannot be analyzed
     * @since 3.9.1
     */
    public JarDataSummary getJarDependencySummary(Artifact artifact) throws IOException {
        JarDataSummary jarDataSummary = dependencySummaries.get(artifact.getId());
        if (jarDataSummary != null) {
            return jarDataSummary;
        }

        File file = getFile(artifact);

        if (file.isDirectory()) {
            jarDataSummary = JarDataSummary.DIRECTORY_JAR_DATA_SUMMARY;
        } else {
            jarDataSummary = summarize(artifact, file);
        }

        dependencySummaries.put(artifact.getId(), jarDataSummary);

        return jarDataSummary;
    }

    private JarData analyze(File file) throws IOException {
        JarAnalyzer jarAnalyzer = new JarAnalyzer(file);

        try {
            classesAnalyzer.analyze(jarAnalyzer);
        } finally {
            jarAnalyzer.closeQuietly();
        }

        return jarAnalyzer.getJarData();
    }

    private JarDataSummary summarize(Artifact artifact, File file) throws IOException {
        Path cacheFile = cacheDirectory == null
                ? null
                : getCacheDirectory(cacheDirectory, artifact).resolve("jar-data.properties");

        BasicFileAttributes fileAttr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        if (cacheFile != null) {
            JarDataSummary cached = readCache(artifact, cacheFile);

            // cheap file change check
            if (cached != null
                    && cached.getFsize() == fileAttr.size()
                    && cached.getTs() == fileAttr.lastModifiedTime().toMillis()) {
                LOG.debug("JarDataSummary cached for: {}", artifact);
                return cached;
            }
        }

        JarDataSummary jarDataSummary = JarDataSummary.fromJarData(analyze(file), fileAttr);

        if (cacheFile != null) {
            writeCache(artifact, cacheFile, jarDataSummary);
        }
        LOG.debug("JarDataSummary analyzed for: {}", artifact);
        return jarDataSummary;
    }

    /**
     * @return the cached summary, or <code>null</code> if it cannot be used: it does not exist yet, is unreadable,
     *         invalid, or was written by a different version of the cache file format. In all those cases the JAR
     *         file is analyzed again.
     */
    private JarDataSummary readCache(Artifact artifact, Path cacheFile) {
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            props.load(reader);
            JarDataSummary cached = JarDataSummary.fromProperties(props);
            if (cached == null) {
                LOG.debug("JarDataSummary cache of a different version ignored for: {}", artifact);
            }
            return cached;
        } catch (NoSuchFileException e) {
            // not cached yet
            return null;
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Loading JarDataSummary from cache failed: {}", artifact, e);
            return null;
        }
    }

    /**
     * Save the summary to the cache for the next build. A failure to do so is not an error: the cache is only an
     * optimization. The file is moved into place once fully written, so that a build running concurrently (which may
     * share the cache directory) never reads a partially written file.
     */
    private void writeCache(Artifact artifact, Path cacheFile, JarDataSummary jarDataSummary) {
        Path tmpFile = null;
        try {
            Files.createDirectories(cacheFile.getParent());
            tmpFile = Files.createTempFile(cacheFile.getParent(), "jar-data", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8)) {
                jarDataSummary.toProperties().store(writer, null);
            }
            try {
                Files.move(tmpFile, cacheFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioe) {
            LOG.warn("Saving JarDataSummary to cache failed: {}", artifact, ioe);
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (IOException ignored) {
                    // nothing more can be done
                }
            }
        }
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
     * root/groupId/artifactId/version/[classifier]
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
