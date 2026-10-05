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
import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the cache file format of {@link JarDataSummary}.
 */
class JarDataSummaryTest {

    private static Properties validProperties() {
        Properties props = new Properties();
        props.setProperty("v", "1");
        props.setProperty("sealed", "true");
        props.setProperty("numEntries", "128");
        props.setProperty("numClasses", "86");
        props.setProperty("numPackages", "7");
        props.setProperty("jdkRevision", "1.8");
        props.setProperty("debugPresent", "true");
        props.setProperty("multiRelease", "true");
        props.setProperty("numRootEntries", "110");
        props.setProperty("fsize", "192368");
        props.setProperty("ts", "1700000000000");
        props.setProperty("versionedRuntimes", "2");
        for (int i = 0; i < 2; i++) {
            props.setProperty("versionedRuntimes." + i + ".debugPresent", "false");
            props.setProperty("versionedRuntimes." + i + ".numEntries", "6");
            props.setProperty("versionedRuntimes." + i + ".numClasses", "1");
            props.setProperty("versionedRuntimes." + i + ".numPackages", "1");
            props.setProperty("versionedRuntimes." + i + ".jdkRevision", String.valueOf(9 + i));
        }
        return props;
    }

    @Test
    void roundTrip() {
        JarDataSummary summary = JarDataSummary.fromProperties(validProperties());
        JarDataSummary copy = JarDataSummary.fromProperties(summary.toProperties());

        assertEquals(validProperties(), copy.toProperties());
        assertEquals(1, copy.getV());
        assertEquals(1, JarDataSummary.DIRECTORY_JAR_DATA_SUMMARY.getV());
        assertTrue(copy.isSealed());
        assertEquals(128, copy.getNumEntries());
        assertEquals(192368L, copy.getFsize());
        assertEquals(1700000000000L, copy.getTs());
        assertEquals(2, copy.getVersionedRuntimes().size());
        assertEquals("10", copy.getVersionedRuntimes().get(1).getJdkRevision());
        assertFalse(copy.getVersionedRuntimes().get(0).isDebugPresent());
    }

    @Test
    void notMultiRelease() {
        Properties props = validProperties();
        props.setProperty("multiRelease", "false");
        for (String name : new ArrayList<>(props.stringPropertyNames())) {
            if (name.startsWith("versionedRuntimes")) {
                props.remove(name);
            }
        }
        props.remove("jdkRevision");

        JarDataSummary summary = JarDataSummary.fromProperties(props);

        assertNull(summary.getVersionedRuntimes());
        assertNull(summary.getJdkRevision());
        assertEquals(props, summary.toProperties());
    }

    @Test
    void differentVersionIsIgnored() {
        Properties props = validProperties();
        props.setProperty("v", "2");

        assertNull(JarDataSummary.fromProperties(props));
    }

    @Test
    void differentVersionIsIgnoredEvenIfTheRestIsUnknown() {
        Properties props = new Properties();
        props.setProperty("v", "2");
        props.setProperty("someFieldOfTheFuture", "1");

        assertNull(JarDataSummary.fromProperties(props));
    }

    @Test
    void invalidFilesAreRejected() {
        for (String missing : Arrays.asList("v", "numEntries", "fsize", "ts", "sealed", "multiRelease")) {
            Properties props = validProperties();
            props.remove(missing);
            assertThrows(IllegalArgumentException.class, () -> JarDataSummary.fromProperties(props), missing);
        }

        Properties malformed = validProperties();
        malformed.setProperty("numClasses", "many");
        assertThrows(IllegalArgumentException.class, () -> JarDataSummary.fromProperties(malformed));

        Properties notABoolean = validProperties();
        notABoolean.setProperty("debugPresent", "maybe");
        assertThrows(IllegalArgumentException.class, () -> JarDataSummary.fromProperties(notABoolean));

        Properties missingRuntime = validProperties();
        missingRuntime.remove("versionedRuntimes.1.numEntries");
        assertThrows(IllegalArgumentException.class, () -> JarDataSummary.fromProperties(missingRuntime));
    }

    /**
     * The count of runtimes comes from a file which is not trusted: it must not be used to allocate memory.
     */
    @Test
    void hugeCountOfRuntimesIsRejectedWithoutAllocatingMemory() {
        Properties props = validProperties();
        props.setProperty("versionedRuntimes", String.valueOf(Integer.MAX_VALUE));

        assertThrows(IllegalArgumentException.class, () -> JarDataSummary.fromProperties(props));
    }

    @Test
    void negativeCountOfRuntimesIsRejected() {
        Properties props = validProperties();
        props.setProperty("versionedRuntimes", "-1");

        assertThrows(IllegalArgumentException.class, () -> JarDataSummary.fromProperties(props));
    }
}
