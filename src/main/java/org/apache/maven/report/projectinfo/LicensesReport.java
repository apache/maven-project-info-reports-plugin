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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.model.License;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;

/**
 * Generates the Project Licenses report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.0
 */
@Mojo(name = "licenses")
public class LicensesReport extends AbstractProjectInfoReport {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Whether the system is currently offline.
     */
    @Parameter(property = "settings.offline")
    private boolean offline;

    /**
     * Whether the only render links to the license documents instead of inlining them.
     * <br/>
     * If the system is in {@link #offline} mode, the linkOnly parameter will be always <code>true</code>.
     * <br/>
     * The user property <code>licenses.linkOnly</code> is available since 3.9.1.
     *
     * @since 2.3
     */
    @Parameter(property = "licenses.linkOnly", defaultValue = "false")
    private boolean linkOnly;

    /**
     * Specifies the input encoding of the project's license file(s).
     *
     * @since 2.8
     */
    @Parameter
    private String licenseFileEncoding;

    @Inject
    public LicensesReport(RepositorySystem repositorySystem, I18N i18n, ProjectBuilder projectBuilder) {
        super(repositorySystem, i18n, projectBuilder);
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        boolean result = super.canGenerateReport();
        if (result && skipEmptyReport) {
            result = !isEmpty(getProject().getModel().getLicenses());
        }

        if (!result) {
            return false;
        }

        if (!offline) {
            return true;
        }

        for (License license : project.getModel().getLicenses()) {
            String url = license.getUrl();

            URL licenseUrl = null;
            try {
                licenseUrl = getLicenseURL(project, url);
            } catch (IOException e) {
                getLog().error(e.getMessage());
            }

            if (licenseUrl != null && licenseUrl.getProtocol().equals("file")) {
                return true;
            }

            if (licenseUrl != null
                    && (licenseUrl.getProtocol().equals("http")
                            || licenseUrl.getProtocol().equals("https"))) {
                linkOnly = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public void executeReport(Locale locale) {
        LicensesRenderer r = new LicensesRenderer(
                getSink(),
                getProject(),
                getI18N(locale),
                locale,
                settings,
                linkOnly,
                licenseFileEncoding,
                getLicenseContentCache());

        r.render();
    }

    /**
     * Key of the per-build cache of downloaded license texts in the {@link SessionData}.
     */
    static final String LICENSE_CONTENT_CACHE_KEY = LicensesReport.class.getName() + ".licenseContent";

    /**
     * Cache key for a license URL: the encoding is normalised the way {@link ProjectInfoReportUtils#getContent}
     * does, so that <code>null</code>, an empty string and <code>UTF-8</code> share one download.
     */
    static String licenseContentCacheKey(URL licenseUrl, String encoding) {
        String normalisedEncoding =
                encoding == null || encoding.isEmpty() ? "UTF-8" : encoding.toUpperCase(Locale.ROOT);
        return licenseUrl.toExternalForm() + '\n' + normalisedEncoding;
    }

    /**
     * Returns the cache of license texts downloaded during this build, keyed by URL and encoding. It lives in the
     * repository session so that every module of a reactor sharing one <code>&lt;licenses&gt;</code> section
     * downloads each URL once instead of once per module (MPIR #586); a failed download is cached as well so the
     * connect timeout is paid once. A plain map is returned when there is no session data, as in some tests.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getLicenseContentCache() {
        RepositorySystemSession repositorySession =
                getSession() == null ? null : getSession().getRepositorySession();
        SessionData data = repositorySession == null ? null : repositorySession.getData();
        if (data == null) {
            return new ConcurrentHashMap<>();
        }
        Object cache = data.get(LICENSE_CONTENT_CACHE_KEY);
        if (cache == null) {
            // compare-and-set: SessionData.computeIfAbsent does not exist in the resolver of Maven 3.6.3
            data.set(LICENSE_CONTENT_CACHE_KEY, null, new ConcurrentHashMap<String, Object>());
            cache = data.get(LICENSE_CONTENT_CACHE_KEY);
        }
        return (Map<String, Object>) cache;
    }

    /**
     * @deprecated use {@link #getOutputPath()} instead
     */
    @Override
    @Deprecated
    public String getOutputName() {
        return getOutputPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputPath() {
        return "licenses";
    }

    @Override
    protected String getI18Nsection() {
        return "licenses";
    }

    /**
     * @param project not null
     * @param url     not null
     * @return a valid URL object from the url string
     * @throws IOException if any
     */
    protected static URL getLicenseURL(MavenProject project, String url) throws IOException {
        URL licenseUrl;
        UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
        // UrlValidator does not accept file URLs because the file
        // URLs do not contain a valid authority (no hostname).
        // As a workaround accept license URLs that start with the
        // file scheme.
        if (urlValidator.isValid(url) || Objects.toString(url, "").startsWith("file://")) {
            try {
                licenseUrl = new URL(url);
            } catch (MalformedURLException e) {
                throw new MalformedURLException("The license url '" + url + "' seems to be invalid: " + e.getMessage());
            }
        } else {
            File licenseFile = new File(project.getBasedir(), url);
            if (!licenseFile.exists()) {
                // Workaround to allow absolute path names while
                // staying compatible with the way it was...
                licenseFile = new File(url);
            }
            if (!licenseFile.exists()) {
                throw new IOException("Maven can't find the file '" + licenseFile + "' on the system.");
            }
            try {
                licenseUrl = licenseFile.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new MalformedURLException("The license url '" + url + "' seems to be invalid: " + e.getMessage());
            }
        }

        return licenseUrl;
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class LicensesRenderer extends AbstractProjectInfoRenderer {
        private final MavenProject project;

        private final Settings settings;

        private final boolean linkOnly;

        private final String licenseFileEncoding;

        /**
         * License text, or the failure message, per URL and encoding, shared across the build.
         */
        private final Map<String, Object> licenseContentCache;

        LicensesRenderer(
                Sink sink,
                MavenProject project,
                I18N i18n,
                Locale locale,
                Settings settings,
                boolean linkOnly,
                String licenseFileEncoding,
                Map<String, Object> licenseContentCache) {
            super(sink, i18n, locale);

            this.project = project;

            this.settings = settings;

            this.linkOnly = linkOnly;

            this.licenseFileEncoding = licenseFileEncoding;

            this.licenseContentCache = licenseContentCache;
        }

        @Override
        protected String getI18Nsection() {
            return "licenses";
        }

        @Override
        protected void renderBody() {
            List<License> licenses = project.getModel().getLicenses();

            if (licenses.isEmpty()) {
                startSection(getTitle());

                paragraph(getI18nString("nolicense"));

                endSection();

                return;
            }

            // Overview
            startSection(getI18nString("overview.title"));

            paragraph(getI18nString("overview.intro"));

            endSection();

            // License
            startSection(getI18nString("title"));

            if (licenses.size() > 1) {
                // multiple licenses
                paragraph(getI18nString("multiple"));

                if (!linkOnly) {
                    // add an index before licenses content
                    sink.list();
                    for (License license : licenses) {
                        String name = license.getName();
                        if (name == null || name.isEmpty()) {
                            name = getI18nString("unnamed");
                        }

                        sink.listItem();
                        link("#" + DoxiaUtils.encodeId(name), name);
                        sink.listItem_();
                    }
                    sink.list_();
                }
            }

            for (License license : licenses) {
                String name = license.getName();
                if (name == null || name.isEmpty()) {
                    name = getI18nString("unnamed");
                }

                String url = license.getUrl();
                String comments = license.getComments();

                startSection(name);

                if (!(comments == null || comments.isEmpty())) {
                    paragraph(comments);
                }

                if (url != null) {
                    try {
                        URL licenseUrl = getLicenseURL(project, url);

                        if (linkOnly) {
                            link(licenseUrl.toExternalForm(), licenseUrl.toExternalForm());
                        } else {
                            renderLicenseContent(licenseUrl);
                        }
                    } catch (IOException e) {
                        // I18N message
                        paragraph(e.getMessage());
                    }
                }

                endSection();
            }

            endSection();
        }

        /**
         * Render the license content into the report.
         *
         * @param licenseUrl the license URL
         */
        private void renderLicenseContent(URL licenseUrl) {
            try {
                // All licenses are supposed to be in English...
                String licenseContent = getLicenseContent(licenseUrl);

                // TODO: we should check for a text/html mime type instead, and possibly use a html parser to do this a
                // bit more cleanly/reliably.
                String licenseContentLC = licenseContent.toLowerCase(Locale.ENGLISH);
                int bodyStart = licenseContentLC.indexOf("<body");
                int bodyEnd = licenseContentLC.indexOf("</body>");

                if ((licenseContentLC.contains("<!doctype html") || licenseContentLC.contains("<html>"))
                        && ((bodyStart >= 0) && (bodyEnd > bodyStart))) {
                    bodyStart = licenseContentLC.indexOf('>', bodyStart) + 1;
                    String body = licenseContent.substring(bodyStart, bodyEnd);

                    link(licenseUrl.toExternalForm(), getI18nString("originalText"));
                    paragraph(getI18nString("copy"));

                    body = replaceRelativeLinks(body, baseURL(licenseUrl).toExternalForm());
                    sink.rawText(body);
                } else {
                    verbatimText(licenseContent);
                }
            } catch (IOException e) {
                paragraph("Can't read the url [" + licenseUrl + "] : " + e.getMessage());
            }
        }

        /**
         * Fetches the license text once per build: later modules asking for the same URL get the cached text, or
         * the cached failure, without another request to the server hosting the license.
         */
        private String getLicenseContent(URL licenseUrl) throws IOException {
            String key = licenseContentCacheKey(licenseUrl, licenseFileEncoding);
            // computeIfAbsent so that modules built in parallel wait for one download instead of each starting one
            Object cached = licenseContentCache.computeIfAbsent(key, k -> {
                try {
                    return ProjectInfoReportUtils.getContent(licenseUrl, settings, licenseFileEncoding);
                } catch (IOException e) {
                    return new Failure(e.getMessage());
                }
            });
            if (cached instanceof Failure) {
                throw new IOException(((Failure) cached).message);
            }
            return (String) cached;
        }

        /** A download that failed earlier in this build; only the message is kept. */
        private static final class Failure {
            private final String message;

            Failure(String message) {
                this.message = message;
            }
        }

        private static URL baseURL(URL aUrl) {
            String urlTxt = aUrl.toExternalForm();
            int lastSlash = urlTxt.lastIndexOf('/');
            if (lastSlash > -1) {
                try {
                    return new URL(urlTxt.substring(0, lastSlash + 1));
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
            }

            return aUrl;
        }

        private static String replaceRelativeLinks(String html, String baseURL) {
            String url = baseURL;
            if (!url.endsWith("/")) {
                url += "/";
            }

            String serverURL = url.substring(0, url.indexOf('/', url.indexOf("//") + 2));

            String content = replaceParts(html, url, serverURL, "[aA]", "[hH][rR][eE][fF]");
            content = replaceParts(content, url, serverURL, "[iI][mM][gG]", "[sS][rR][cC]");
            return content;
        }

        private static String replaceParts(
                String html, String baseURL, String serverURL, String tagPattern, String attributePattern) {
            Pattern anchor = Pattern.compile(
                    "(<\\s*" + tagPattern + "\\s+[^>]*" + attributePattern + "\\s*=\\s*\")([^\"]*)\"([^>]*>)");
            StringBuilder sb = new StringBuilder(html);

            int indx = 0;
            boolean done = false;
            while (!done) {
                Matcher mAnchor = anchor.matcher(sb);
                if (mAnchor.find(indx)) {
                    indx = mAnchor.end(3);

                    if (mAnchor.group(2).startsWith("#")) {
                        // relative link - don't want to alter this one!
                    }
                    if (mAnchor.group(2).startsWith("/")) {
                        // root link
                        sb.insert(mAnchor.start(2), serverURL);
                        indx += serverURL.length();
                    } else if (mAnchor.group(2).indexOf(':') < 0) {
                        // relative link
                        sb.insert(mAnchor.start(2), baseURL);
                        indx += baseURL.length();
                    }
                } else {
                    done = true;
                }
            }
            return sb.toString();
        }
    }
}
