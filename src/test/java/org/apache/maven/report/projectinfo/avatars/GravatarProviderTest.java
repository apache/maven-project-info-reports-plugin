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
package org.apache.maven.report.projectinfo.avatars;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GravatarProviderTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void urlShouldBeCorrect() {
        GravatarProvider gravatarProvider = new GravatarProvider();
        gravatarProvider.setBaseUrl("https://www.gravatar.com/avatar");
        String externalAvatarUrl = gravatarProvider.getAvatarUrl("email@example.com");

        assertEquals(
                "https://www.gravatar.com/avatar/5658ffccee7f0ebfda2b226238b1eb6e.jpg?d=blank&s=60", externalAvatarUrl);
    }

    @Test
    public void urlForEmptyEmailShouldBeCorrect() {
        GravatarProvider gravatarProvider = new GravatarProvider();
        gravatarProvider.setBaseUrl("https://www.gravatar.com/avatar");

        String externalAvatarUrl = gravatarProvider.getAvatarUrl(null);

        assertEquals(
                "https://www.gravatar.com/avatar/00000000000000000000000000000000.jpg?d=blank&f=y&s=60",
                externalAvatarUrl);

        externalAvatarUrl = gravatarProvider.getAvatarUrl("");

        assertEquals(
                "https://www.gravatar.com/avatar/00000000000000000000000000000000.jpg?d=blank&f=y&s=60",
                externalAvatarUrl);
    }

    @Test
    public void localAvatarPathShouldBeCorrect() throws Exception {
        GravatarProvider gravatarProvider = new GravatarProvider();
        gravatarProvider.setBaseUrl("https://www.gravatar.com/avatar");
        gravatarProvider.setOutputDirectory(tmpFolder.getRoot());
        String localAvatarUrl = gravatarProvider.getLocalAvatarPath("sjaranowski@apache.org");
        assertEquals("avatars/90cc13b765c79d2d55ca64388ea2bc5f.jpg", localAvatarUrl);
        assertTrue(new File(tmpFolder.getRoot(), "avatars/90cc13b765c79d2d55ca64388ea2bc5f.jpg").exists());
    }

    @Test
    public void localAvatarPathShouldHaveDefaultForNotExisting() throws Exception {
        GravatarProvider gravatarProvider = new GravatarProvider();
        gravatarProvider.setBaseUrl("https://www.gravatar.com/avatar");
        gravatarProvider.setOutputDirectory(tmpFolder.getRoot());
        String localAvatarUrl = gravatarProvider.getLocalAvatarPath("test@example.com");
        assertEquals("avatars/00000000000000000000000000000000.jpg", localAvatarUrl);
        assertTrue(new File(tmpFolder.getRoot(), "avatars/00000000000000000000000000000000.jpg").exists());
    }

    @Test
    public void localAvatarPathShouldBeCorrectForDefault() throws Exception {
        GravatarProvider gravatarProvider = new GravatarProvider();
        gravatarProvider.setBaseUrl("https://www.gravatar.com/avatar");
        gravatarProvider.setOutputDirectory(tmpFolder.getRoot());
        String localAvatarUrl = gravatarProvider.getLocalAvatarPath(null);
        assertEquals("avatars/00000000000000000000000000000000.jpg", localAvatarUrl);
        assertTrue(new File(tmpFolder.getRoot(), "avatars/00000000000000000000000000000000.jpg").exists());
    }
}
