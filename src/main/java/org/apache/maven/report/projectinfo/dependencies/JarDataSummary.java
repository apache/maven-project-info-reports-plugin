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
import java.util.Properties;

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
     * version to save in the cache file; a cache file written with any other version is ignored
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
    private final int v;

    /**
     * is sealed
     */
    private final boolean sealedJar;

    private final int numEntries;

    private final int numClasses;

    private final int numPackages;

    private final String jdkRevision;

    private final boolean debugPresent;

    private final boolean multiRelease;

    private final List<VersionedRuntime> versionedRuntimes;

    private final int numRootEntries;

    /**
     * file size (fsize) and last modification timestamp (ts) as a cheap file change detector
     */
    private final long fsize;

    private final long ts;

    private JarDataSummary(
            int v,
            boolean sealedJar,
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
        this.sealedJar = sealedJar;
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
        return sealedJar;
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
     * Convert this summary to the flat key/value form which is stored in the cache file. The entries of the
     * multi-release runtimes are stored as <code>versionedRuntimes.<i>n</i>.<i>field</i></code>.
     *
     * @return the properties representing this summary, never <code>null</code>.
     * @see #fromProperties(Properties)
     */
    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("v", String.valueOf(v));
        props.setProperty("sealed", String.valueOf(sealedJar));
        props.setProperty("numEntries", String.valueOf(numEntries));
        props.setProperty("numClasses", String.valueOf(numClasses));
        props.setProperty("numPackages", String.valueOf(numPackages));
        if (jdkRevision != null) {
            props.setProperty("jdkRevision", jdkRevision);
        }
        props.setProperty("debugPresent", String.valueOf(debugPresent));
        props.setProperty("multiRelease", String.valueOf(multiRelease));
        props.setProperty("numRootEntries", String.valueOf(numRootEntries));
        props.setProperty("fsize", String.valueOf(fsize));
        props.setProperty("ts", String.valueOf(ts));
        if (versionedRuntimes != null) {
            props.setProperty("versionedRuntimes", String.valueOf(versionedRuntimes.size()));
            for (int i = 0; i < versionedRuntimes.size(); i++) {
                VersionedRuntime runtime = versionedRuntimes.get(i);
                String prefix = "versionedRuntimes." + i + '.';
                props.setProperty(prefix + "debugPresent", String.valueOf(runtime.isDebugPresent()));
                props.setProperty(prefix + "numEntries", String.valueOf(runtime.getNumEntries()));
                props.setProperty(prefix + "numClasses", String.valueOf(runtime.getNumClasses()));
                props.setProperty(prefix + "numPackages", String.valueOf(runtime.getNumPackages()));
                if (runtime.getJdkRevision() != null) {
                    props.setProperty(prefix + "jdkRevision", runtime.getJdkRevision());
                }
            }
        }
        return props;
    }

    /**
     * Create a JarDataSummary from the contents of a cache file.
     *
     * @param props the properties read from the cache file.
     * @return the summary, or <code>null</code> if the properties were written with a version of the cache file
     *         format which is not the current one (in which case they must not be used).
     * @throws IllegalArgumentException if the properties are not a valid cache file (a field is missing or
     *         malformed).
     * @see #toProperties()
     */
    public static JarDataSummary fromProperties(Properties props) {
        if (getInt(props, "v") != VERSION) {
            return null;
        }

        List<VersionedRuntime> versionedRuntimes = null;
        if (props.getProperty("versionedRuntimes") != null) {
            int count = getInt(props, "versionedRuntimes");
            if (count < 0) {
                throw new IllegalArgumentException("Malformed field 'versionedRuntimes'");
            }
            // the list is deliberately not sized from the count: it comes from a file which is not trusted, and a
            // count larger than the entries present fails on the first missing one
            versionedRuntimes = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String prefix = "versionedRuntimes." + i + '.';
                versionedRuntimes.add(new VersionedRuntime(
                        getBoolean(props, prefix + "debugPresent"),
                        getInt(props, prefix + "numEntries"),
                        getInt(props, prefix + "numClasses"),
                        getInt(props, prefix + "numPackages"),
                        props.getProperty(prefix + "jdkRevision")));
            }
        }

        return new JarDataSummary(
                VERSION,
                getBoolean(props, "sealed"),
                getInt(props, "numEntries"),
                getInt(props, "numClasses"),
                getInt(props, "numPackages"),
                props.getProperty("jdkRevision"),
                getBoolean(props, "debugPresent"),
                getBoolean(props, "multiRelease"),
                versionedRuntimes,
                getInt(props, "numRootEntries"),
                getLong(props, "fsize"),
                getLong(props, "ts"));
    }

    private static String getRequired(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field '" + key + "'");
        }
        return value.trim();
    }

    private static int getInt(Properties props, String key) {
        try {
            return Integer.parseInt(getRequired(props, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed field '" + key + "'", e);
        }
    }

    private static long getLong(Properties props, String key) {
        try {
            return Long.parseLong(getRequired(props, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed field '" + key + "'", e);
        }
    }

    private static boolean getBoolean(Properties props, String key) {
        String value = getRequired(props, key);
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("Malformed field '" + key + "'");
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Create a new JarDataSummary from the contents of the jarData argument.
     *
     * @param jarData the JAR data contents.
     * @param fileAttributes the attributes of the JAR file, kept to detect later if it has changed.
     * @return a new instance of JarDataSummary.
     */
    public static JarDataSummary fromJarData(JarData jarData, BasicFileAttributes fileAttributes) {
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
                fileAttributes.size(),
                fileAttributes.lastModifiedTime().toMillis());
    }

    /**
     * Summary information for multi-release JAR
     */
    public static class VersionedRuntime {
        private final boolean debugPresent;

        private final int numEntries;

        private final int numClasses;

        private final int numPackages;

        private final String jdkRevision;

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
