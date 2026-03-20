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

import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClasses;
import org.apache.maven.shared.jar.classes.JarVersionedRuntime;

/**
 * Summary data for the file details
 *
 * @since 3.9.1
 */
public class JarDataSummary {

    /**
     * version to save in the cache file
     */
    private static final int VERSION = 1;

    /**
     * Summary for directories.
     */
    public static final JarDataSummary DIRECTORY_JAR_DATA_SUMMARY =
            new JarDataSummary(VERSION, false, 0, 0, 0, null, false, false, null, 0, 0, 0);

    /**
     * version of the serialized object (to support backward/forward compatibility in the future) Simplest version type
     * with int. No need to use semver strings for this.
     */
    private int v;

    /**
     * is sealed
     */
    @JsonProperty("sealed")
    private boolean aSealed;

    private int numEntries;

    private int numClasses;

    private int numPackages;

    private String jdkRevision;

    private boolean debugPresent;

    private boolean multiRelease;

    private List<VersionedRuntime> versionedRuntimes;

    private int numRootEntries;

    /**
     * file size (fsize) and creation timestamp (ts) as a cheap file change detector
     */
    private long fsize;

    private long ts;

    private JarDataSummary() {}

    private JarDataSummary(
            int v,
            boolean aSealed,
            int numEntries,
            int numClasses,
            int numPackages,
            String jdkRevision,
            boolean debugPresent,
            boolean multiRelease,
            List<VersionedRuntime> versionedRuntimes,
            int numRootEntries,
            long fsize,
            long ts) {
        super();
        this.v = v;
        this.aSealed = aSealed;
        this.numEntries = numEntries;
        this.numClasses = numClasses;
        this.numPackages = numPackages;
        this.jdkRevision = jdkRevision;
        this.debugPresent = debugPresent;
        this.multiRelease = multiRelease;
        this.versionedRuntimes = versionedRuntimes;
        this.numRootEntries = numRootEntries;
        this.fsize = fsize;
        this.ts = ts;
    }

    /**
     * Get the version field.
     *
     * @return the version field.
     */
    public int getV() {
        return v;
    }

    /**
     * Get if the JAR is sealed.
     *
     * @return return true if it is sealed.
     */
    public boolean isSealed() {
        return aSealed;
    }

    /**
     * Get the number of entries in the JAR.
     *
     * @return the number of entries.
     */
    public int getNumEntries() {
        return numEntries;
    }

    /**
     * Get the number of classes in the JAR.
     *
     * @return the number of classes.
     */
    public int getNumClasses() {
        return numClasses;
    }

    /**
     * Get the number of packages in the JAR.
     *
     * @return the number of packages.
     */
    public int getNumPackages() {
        return numPackages;
    }

    /**
     * Get the JDK Revision of the JAR.
     *
     * @return a String with the JDK revision.
     */
    public String getJdkRevision() {
        return jdkRevision;
    }

    /**
     * Check if there is debug information present.
     *
     * @return true if there is debug information present.
     */
    public boolean isDebugPresent() {
        return debugPresent;
    }

    /**
     * Check if the JAR is multi-release.
     *
     * @return true if it is multi-release.
     */
    public boolean isMultiRelease() {
        return multiRelease;
    }

    /**
     * Get a list of objects representing each version in the multi-release JAR.
     *
     * @return the list of versions.
     */
    public List<VersionedRuntime> getVersionedRuntimes() {
        return versionedRuntimes;
    }

    /**
     * Get the number of entries in the root of the JAR.
     *
     * @return the number of entries.
     */
    public int getNumRootEntries() {
        return numRootEntries;
    }

    /**
     * Get the file size of the JAR file.
     *
     * @return the file size.
     */
    public long getFsize() {
        return fsize;
    }

    /**
     * Get the timestamp of last of last modification of the JAR file.
     *
     * @return the timestamp as long.
     */
    public long getTs() {
        return ts;
    }

    /**
     * Set the attributes used for checking if file has changed.
     *
     * @param fattr the File Attributes to set.
     */
    public void setFileAttributes(BasicFileAttributes fattr) {
        this.fsize = fattr.size();
        this.ts = fattr.lastModifiedTime().toMillis();
    }

    /**
     * Create a new JarDataSummary from the contents of the jarData argument.
     *
     * @param jarData the JAR data contents.
     * @return a new instance of JarDataSummary.
     */
    public static JarDataSummary fromJarData(JarData jarData) {
        List<VersionedRuntime> versionedRuntimes = null;
        if (jarData.isMultiRelease()) {
            Collection<JarVersionedRuntime> jarVersionedRuntimes =
                    jarData.getVersionedRuntimes().getVersionedRuntimeMap().values();
            versionedRuntimes = new ArrayList<>(jarVersionedRuntimes.size());
            for (JarVersionedRuntime jvr : jarVersionedRuntimes) {
                JarClasses jarClasses = jvr.getJarClasses();
                versionedRuntimes.add(new VersionedRuntime(
                        jvr.getJarClasses().isDebugPresent(),
                        jvr.getNumEntries(),
                        jarClasses.getClassNames().size(),
                        jarClasses.getPackages().size(),
                        jarClasses.getJdkRevision()));
            }
        }
        return new JarDataSummary(
                VERSION,
                jarData.isSealed(),
                jarData.getNumEntries(),
                jarData.getNumClasses(),
                jarData.getNumPackages(),
                jarData.getJdkRevision(),
                jarData.isDebugPresent(),
                jarData.isMultiRelease(),
                versionedRuntimes,
                jarData.getRootEntries() == null ? 0 : jarData.getNumRootEntries(),
                0,
                0);
    }

    /**
     * Summary information for multi-release JAR
     */
    public static class VersionedRuntime {
        private boolean debugPresent;

        private int numEntries;

        private int numClasses;

        private int numPackages;

        private String jdkRevision;

        /**
         * Default constructor
         */
        public VersionedRuntime() {}

        /**
         * The constructor with all attributes.
         *
         * @param debugPresent is debug present
         * @param numEntries the number of entries
         * @param numClasses the number of classes
         * @param numPackages the number of packages
         * @param jdkRevision the JDK revision
         */
        public VersionedRuntime(
                boolean debugPresent, int numEntries, int numClasses, int numPackages, String jdkRevision) {
            super();
            this.debugPresent = debugPresent;
            this.numEntries = numEntries;
            this.numClasses = numClasses;
            this.numPackages = numPackages;
            this.jdkRevision = jdkRevision;
        }

        /**
         * Check if there is debug information present.
         *
         * @return true if there is debug information present.
         */
        public boolean isDebugPresent() {
            return debugPresent;
        }

        /**
         * Get the number of entries in the JAR.
         *
         * @return the number of entries.
         */
        public int getNumEntries() {
            return numEntries;
        }

        /**
         * Get the number of classes in the JAR.
         *
         * @return the number of classes.
         */
        public int getNumClasses() {
            return numClasses;
        }

        /**
         * Get the number of packages in the JAR.
         *
         * @return the number of packages.
         */
        public int getNumPackages() {
            return numPackages;
        }

        /**
         * Get the JDK Revision of the JAR.
         *
         * @return a String with the JDK revision.
         */
        public String getJdkRevision() {
            return jdkRevision;
        }
    }
}
