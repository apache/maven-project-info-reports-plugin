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

import javax.inject.Named;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for user avatar from gravatar.com
 * <p>
 * <a href="https://docs.gravatar.com/api/avatars/images/">Gravatar API</a>
 */
@Named("gravatar")
class GravatarProvider implements AvatarsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GravatarProvider.class);

    private static final String AVATAR_SIZE = "s=60";

    private static final String AVATAR_DIRECTORY = "avatars";

    private static final String AVATAR_DEFAULT_FILE_NAME = "00000000000000000000000000000000.jpg";

    private String baseUrl = "https://www.gravatar.com/avatar/";

    private Path outputDirectory;

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Override
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory.toPath();
    }

    public String getAvatarUrl(String email) {
        return getAvatarUrl(email, "blank");
    }

    private String getAvatarUrl(String email, String defaultAvatar) {
        if (email == null || email.isEmpty()) {
            return getSpacerGravatarUrl();
        }

        try {
            email = email.trim().toLowerCase(Locale.ROOT);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(email.getBytes());
            byte[] byteData = md.digest();
            StringBuilder sb = new StringBuilder();
            final int lowerEightBitsOnly = 0xff;
            for (byte aByteData : byteData) {
                sb.append(Integer.toString((aByteData & lowerEightBitsOnly) + 0x100, 16)
                        .substring(1));
            }
            return baseUrl + sb + ".jpg?d=" + defaultAvatar + "&" + AVATAR_SIZE;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Error while getting MD5 hash, use default image: {}", e.getMessage());
            return getSpacerGravatarUrl();
        }
    }

    @Override
    public String getLocalAvatarPath(String email) throws IOException {
        // use 404 http status for not existing avatars
        String avatarUrl = getAvatarUrl(email, "404");
        try {
            URL url = new URI(avatarUrl).toURL();
            Path name = Paths.get(url.getPath()).getFileName();
            if (AVATAR_DEFAULT_FILE_NAME.equals(name.toString())) {
                copyDefault();
            } else {
                copyUrl(url, outputDirectory.resolve(AVATAR_DIRECTORY).resolve(name));
            }
            return AVATAR_DIRECTORY + "/" + name;
        } catch (URISyntaxException | IOException e) {
            if (e instanceof FileNotFoundException) {
                LOGGER.debug(
                        "Error while getting external avatar url for: {}, use default image: {}:{}",
                        email,
                        e.getClass().getName(),
                        e.getMessage());
            } else {
                LOGGER.warn(
                        "Error while getting external avatar url for: {}, use default image: {}:{}",
                        email,
                        e.getClass().getName(),
                        e.getMessage());
            }
            copyDefault();
            return AVATAR_DIRECTORY + "/" + AVATAR_DEFAULT_FILE_NAME;
        }
    }

    private String getSpacerGravatarUrl() {
        return baseUrl + AVATAR_DEFAULT_FILE_NAME + "?d=blank&f=y&" + AVATAR_SIZE;
    }

    private void copyUrl(URL url, Path outputPath) throws IOException {
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath.getParent());
            try (InputStream in = url.openStream();
                    OutputStream out = Files.newOutputStream(outputPath)) {
                LOGGER.debug("Copying URL {} to {}", url, outputPath);
                IOUtil.copy(in, out);
            }
        }
    }

    private void copyDefault() throws IOException {
        Path outputPath = outputDirectory.resolve(AVATAR_DIRECTORY).resolve(AVATAR_DEFAULT_FILE_NAME);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath.getParent());
            try (InputStream in = getClass().getResourceAsStream("default-avatar.jpg");
                    OutputStream out = Files.newOutputStream(outputPath)) {
                IOUtil.copy(in, out);
            }
        }
    }
}
