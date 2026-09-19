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
package org.apache.maven.report.projectinfo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyStore;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.report.projectinfo.ProjectInfoReportUtils.getArchiveServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:vincent.siveton@crim.ca">Vincent Siveton</a>
 * @version $Id$
 */
class ProjectInfoReportUtilsTest {
    private int port = -1;

    private Settings settingsStub;

    private HttpServer httpServer;

    @BeforeEach
    void setUp() throws Exception {
        org.apache.maven.settings.Server server = new org.apache.maven.settings.Server();
        server.setId("localhost");
        server.setUsername("admin");
        server.setPassword("admin");
        settingsStub = new Settings();
        settingsStub.addServer(server);
    }

    private MavenProject getMavenProjectStub(boolean https) {
        final DistributionManagement distributionManagement = new DistributionManagement();
        DeploymentRepository repository = new DeploymentRepository();
        repository.setId("localhost");
        repository.setUrl((https ? "https" : "http") + "://localhost:" + port);
        distributionManagement.setRepository(repository);
        distributionManagement.setSnapshotRepository(repository);
        return new MavenProjectStub() {
            @Override
            public DistributionManagement getDistributionManagement() {
                return distributionManagement;
            }
        };
    }

    @Test
    void testGetInputStreamURL() throws Exception {
        assertTrue(ProjectInfoReportUtils.isArtifactUrlValid("http://my.intern.domain:8080/test"));

        // file
        URL url = new File(getBasedir(), "/target/classes/project-info-reports.properties")
                .toURI()
                .toURL();

        String content = ProjectInfoReportUtils.getContent(url, getMavenProjectStub(false), settingsStub, "ISO-8859-1");
        assertNotNull(content);
        assertTrue(content.contains("Licensed to the Apache Software Foundation"));

        // file
        url = new File(getBasedir(), "/src/test/resources/iso-8859-5-encoded.txt")
                .toURI()
                .toURL();

        content = ProjectInfoReportUtils.getContent(url, getMavenProjectStub(false), settingsStub, "ISO-8859-5");
        assertNotNull(content);
        assertTrue(content.contains("Свобода всем народам!"));

        // http + no auth
        startServer(false, false);

        url = new URL("http://localhost:" + port + "/project-info-reports.properties");

        content = ProjectInfoReportUtils.getContent(url, getMavenProjectStub(false), settingsStub, "ISO-8859-1");
        assertNotNull(content);
        assertTrue(content.contains("Licensed to the Apache Software Foundation"));

        stopServer();

        // http + auth
        startServer(false, true);

        url = new URL("http://localhost:" + port + "/project-info-reports.properties");

        content = ProjectInfoReportUtils.getContent(url, getMavenProjectStub(false), settingsStub, "ISO-8859-1");
        assertNotNull(content);
        assertTrue(content.contains("Licensed to the Apache Software Foundation"));

        stopServer();

        // https + no auth
        startServer(true, false);

        url = new URL("https://localhost:" + port + "/project-info-reports.properties");

        content = ProjectInfoReportUtils.getContent(url, getMavenProjectStub(true), settingsStub, "ISO-8859-1");
        assertNotNull(content);
        assertTrue(content.contains("Licensed to the Apache Software Foundation"));

        stopServer();

        // https + auth
        startServer(true, true);

        url = new URL("https://localhost:" + port + "/project-info-reports.properties");

        content = ProjectInfoReportUtils.getContent(url, getMavenProjectStub(true), settingsStub, "ISO-8859-1");
        assertNotNull(content);
        assertTrue(content.contains("Licensed to the Apache Software Foundation"));

        stopServer();

        // TODO need to test with a proxy
    }

    @Test
    void testGetUserAgent() {
        String userAgent = ProjectInfoReportUtils.getUserAgent();
        assertTrue(userAgent.startsWith("maven-project-info-reports-plugin/"), userAgent);
        assertTrue(
                userAgent.endsWith("(+https://maven.apache.org/plugins/maven-project-info-reports-plugin/)"),
                userAgent);
    }

    @Test
    void testGetArchiveServer() {
        assertEquals("???UNKNOWN???", getArchiveServer(null));

        assertNull(getArchiveServer(""));

        assertEquals(
                "mail-archives.apache.org",
                getArchiveServer("https://mail-archives.apache.org/mod_mbox/maven-announce/"));

        assertEquals(
                "mail-archives.apache.org",
                getArchiveServer("https://mail-archives.apache.org/mod_mbox/maven-announce/"));

        assertEquals(
                "mail-archives.apache.org",
                getArchiveServer("https://mail-archives.apache.org/mod_mbox/maven-announce"));

        assertEquals(
                "www.mail-archive.com", getArchiveServer("https://www.mail-archive.com/announce@maven.apache.org"));

        assertEquals("www.nabble.com", getArchiveServer("https://www.nabble.com/Maven-Announcements-f15617.html"));

        assertEquals("maven.announce.markmail.org", getArchiveServer("http://maven.announce.markmail.org/"));

        assertEquals("maven.announce.markmail.org", getArchiveServer("http://maven.announce.markmail.org"));
    }

    /**
     * Starts a local server on an ephemeral port that serves the files under {@code target/classes}, the same
     * document root the Jetty fixture used before, optionally over TLS with the key pair that
     * {@code keytool-maven-plugin} generates at {@code target/jetty.jks} and optionally behind basic auth for
     * the {@code admin} user that {@link #setUp()} puts into the settings stub.
     */
    private void startServer(boolean isSSL, boolean withAuth) throws Exception {
        InetSocketAddress address = new InetSocketAddress("localhost", 0);
        if (isSSL) {
            HttpsServer httpsServer = HttpsServer.create(address, 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(createSslContext()));
            httpServer = httpsServer;
        } else {
            httpServer = HttpServer.create(address, 0);
        }

        final File documentRoot = new File(getBasedir(), "target/classes");
        HttpHandler handler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                File file = new File(documentRoot, exchange.getRequestURI().getPath());
                if (!file.isFile()) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                byte[] body = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        };
        HttpContext context = httpServer.createContext("/", handler);

        if (withAuth) {
            context.setAuthenticator(new BasicAuthenticator("MyRealm") {
                @Override
                public boolean checkCredentials(String username, String password) {
                    return "admin".equals(username) && "admin".equals(password);
                }
            });
        }

        httpServer.start();

        port = httpServer.getAddress().getPort();
    }

    private void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);

            httpServer = null;

            port = -1;
        }
    }

    private SSLContext createSslContext() throws Exception {
        char[] password = "apache".toCharArray();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = new FileInputStream(new File(getBasedir(), "target/jetty.jks"))) {
            keyStore.load(in, password);
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }
}
