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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.git.repository.GitScmProviderRepository;
import org.apache.maven.scm.provider.hg.repository.HgScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the Project Source Code Management (SCM) report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @since 2.0
 */
@Mojo(name = "scm")
public class ScmReport extends AbstractProjectInfoReport {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The directory name to check out right after the SCM URL.
     */
    @Parameter(defaultValue = "${project.artifactId}")
    private String checkoutDirectoryName;

    /**
     * The SCM anonymous connection url respecting the SCM URL Format.
     *
     * @see <a href="http://maven.apache.org/scm/scm-url-format.html">SCM URL Format</a>
     * @since 2.1
     */
    @Parameter(defaultValue = "${project.scm.connection}")
    private String anonymousConnection;

    /**
     * The SCM developer connection url respecting the SCM URL Format.
     *
     * @see <a href="http://maven.apache.org/scm/scm-url-format.html">SCM URL Format</a>
     * @since 2.1
     */
    @Parameter(defaultValue = "${project.scm.developerConnection}")
    private String developerConnection;

    /**
     * The SCM web access url.
     *
     * @since 2.1
     */
    @Parameter(defaultValue = "${project.scm.url}")
    private String webAccessUrl;

    /**
     * The SCM tag.
     *
     * @since 2.8
     */
    @Parameter(defaultValue = "${project.scm.tag}")
    private String scmTag;

    /**
     * Maven SCM Manager.
     */
    protected final ScmManager scmManager;

    @Inject
    public ScmReport(
            RepositorySystem repositorySystem, I18N i18n, ProjectBuilder projectBuilder, ScmManager scmManager) {
        super(repositorySystem, i18n, projectBuilder);
        this.scmManager = scmManager;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        boolean result = super.canGenerateReport();
        if (result && skipEmptyReport) {
            Scm scm = getProject().getModel().getScm();
            result = scm != null;

            if (result
                    && (anonymousConnection == null || anonymousConnection.isEmpty())
                    && (developerConnection == null || developerConnection.isEmpty())
                    && StringUtils.isEmpty(scm.getUrl())) {
                result = false;
            }
        }

        return result;
    }

    @Override
    public void executeReport(Locale locale) {
        ScmRenderer r = new ScmRenderer(
                getLog(),
                scmManager,
                getSink(),
                getProject().getModel(),
                getI18N(locale),
                locale,
                checkoutDirectoryName,
                webAccessUrl,
                anonymousConnection,
                developerConnection,
                scmTag);

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName() {
        return "scm";
    }

    @Override
    protected String getI18Nsection() {
        return "scm";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class ScmRenderer extends AbstractProjectInfoRenderer {
        private static final String LS = System.lineSeparator();

        private Log log;

        private Model model;

        private ScmManager scmManager;

        /**
         * To support more SCM
         */
        private String anonymousConnection;

        private String devConnection;

        private String checkoutDirectoryName;

        private String webAccessUrl;

        private String scmTag;

        ScmRenderer(
                Log log,
                ScmManager scmManager,
                Sink sink,
                Model model,
                I18N i18n,
                Locale locale,
                String checkoutDirName,
                String webAccessUrl,
                String anonymousConnection,
                String devConnection,
                String scmTag) {
            super(sink, i18n, locale);

            this.log = log;

            this.scmManager = scmManager;

            this.model = model;

            this.checkoutDirectoryName = checkoutDirName;

            this.webAccessUrl = webAccessUrl;

            this.anonymousConnection = anonymousConnection;

            this.devConnection = devConnection;

            this.scmTag = scmTag;
        }

        @Override
        protected String getI18Nsection() {
            return "scm";
        }

        @Override
        protected void renderBody() {
            Scm scm = model.getScm();
            if (scm == null
                    || (anonymousConnection == null || anonymousConnection.isEmpty())
                            && (devConnection == null || devConnection.isEmpty())
                            && StringUtils.isEmpty(scm.getUrl())) {
                startSection(getTitle());

                paragraph(getI18nString("noscm"));

                endSection();

                return;
            }

            ScmRepository anonymousRepository = getScmRepository(anonymousConnection);
            ScmRepository devRepository = getScmRepository(devConnection);

            // Overview section
            renderOverviewSection(anonymousRepository, devRepository);

            // Web access section
            renderWebAccessSection(webAccessUrl);

            // Anonymous access section if needed
            renderAnonymousAccessSection(anonymousRepository);

            // Developer access section
            renderDeveloperAccessSection(devRepository);

            // Access from behind a firewall section if needed
            renderAccessBehindFirewallSection(devRepository);

            // Access through a proxy section if needed
            renderAccessThroughProxySection(anonymousRepository, devRepository);
        }

        /**
         * Render the overview section
         *
         * @param anonymousRepository the anonymous repository
         * @param devRepository the developer repository
         */
        private void renderOverviewSection(ScmRepository anonymousRepository, ScmRepository devRepository) {
            startSection(getI18nString("overview.title"));

            if (isScmSystem(anonymousRepository, "git") || isScmSystem(devRepository, "git")) {
                sink.paragraph();
                linkPatternedText(getI18nString("git.intro"));
                sink.paragraph_();
            } else if (isScmSystem(anonymousRepository, "hg") || isScmSystem(devRepository, "hg")) {
                sink.paragraph();
                linkPatternedText(getI18nString("hg.intro"));
                sink.paragraph_();
            } else if (isScmSystem(anonymousRepository, "svn") || isScmSystem(devRepository, "svn")) {
                sink.paragraph();
                linkPatternedText(getI18nString("svn.intro"));
                sink.paragraph_();
            } else {
                paragraph(getI18nString("general.intro"));
            }

            endSection();
        }

        /**
         * Render the web access section
         *
         * @param scmUrl The URL to the project's browsable repository.
         */
        private void renderWebAccessSection(String scmUrl) {
            startSection(getI18nString("webaccess.title"));

            if (scmUrl == null || scmUrl.isEmpty()) {
                paragraph(getI18nString("webaccess.nourl"));
            } else {
                paragraph(getI18nString("webaccess.url"));

                verbatimLink(scmUrl, scmUrl);
            }

            endSection();
        }

        /**
         * Render the anonymous access section depending the repository.
         * <p>
         * Note: ClearCase, Starteam et Perforce seems to have no anonymous access.
         * </p>
         *
         * @param anonymousRepository the anonymous repository
         */
        private void renderAnonymousAccessSection(ScmRepository anonymousRepository) {
            if (anonymousConnection == null || anonymousConnection.isEmpty()) {
                return;
            }

            startSection(getI18nString("anonymousaccess.title"));

            if (anonymousRepository != null && isScmSystem(anonymousRepository, "git")) {
                GitScmProviderRepository gitRepo =
                        (GitScmProviderRepository) anonymousRepository.getProviderRepository();

                anonymousAccessGit(gitRepo);
            } else if (anonymousRepository != null && isScmSystem(anonymousRepository, "hg")) {
                HgScmProviderRepository hgRepo = (HgScmProviderRepository) anonymousRepository.getProviderRepository();

                anonymousAccessMercurial(hgRepo);
            } else if (anonymousRepository != null && isScmSystem(anonymousRepository, "svn")) {
                SvnScmProviderRepository svnRepo =
                        (SvnScmProviderRepository) anonymousRepository.getProviderRepository();

                anonymousAccessSubversion(svnRepo);
            } else {
                paragraph(getI18nString("anonymousaccess.general.intro"));

                verbatimText(anonymousConnection.substring(4));
            }

            endSection();
        }

        /**
         * Render the developer access section
         *
         * @param devRepository the dev repository
         */
        private void renderDeveloperAccessSection(ScmRepository devRepository) {
            if (devConnection == null || devConnection.isEmpty()) {
                return;
            }

            startSection(getI18nString("devaccess.title"));

            if (devRepository != null && isScmSystem(devRepository, "git")) {
                GitScmProviderRepository gitRepo = (GitScmProviderRepository) devRepository.getProviderRepository();

                developerAccessGit(gitRepo);
            } else if (devRepository != null && isScmSystem(devRepository, "hg")) {
                HgScmProviderRepository hgRepo = (HgScmProviderRepository) devRepository.getProviderRepository();

                developerAccessMercurial(hgRepo);
            } else if (devRepository != null && isScmSystem(devRepository, "svn")) {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) devRepository.getProviderRepository();

                developerAccessSubversion(svnRepo);
            } else {
                paragraph(getI18nString("devaccess.general.intro"));

                verbatimText(devConnection.substring(4));
            }

            endSection();
        }

        /**
         * Render the access from behind a firewall section
         *
         * @param devRepository the dev repository
         */
        private void renderAccessBehindFirewallSection(ScmRepository devRepository) {
            startSection(getI18nString("accessbehindfirewall.title"));

            if (devRepository != null && isScmSystem(devRepository, "svn")) {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) devRepository.getProviderRepository();

                paragraph(getI18nString("accessbehindfirewall.svn.intro"));

                verbatimText("$ svn checkout " + svnRepo.getUrl() + " " + checkoutDirectoryName);
            } else {
                paragraph(getI18nString("accessbehindfirewall.general.intro"));
            }

            endSection();
        }

        /**
         * Render the access from behind a firewall section
         *
         * @param anonymousRepository the anonymous repository
         * @param devRepository the dev repository
         */
        private void renderAccessThroughProxySection(ScmRepository anonymousRepository, ScmRepository devRepository) {
            if (isScmSystem(anonymousRepository, "svn") || isScmSystem(devRepository, "svn")) {
                startSection(getI18nString("accessthroughtproxy.title"));

                paragraph(getI18nString("accessthroughtproxy.svn.intro1"));
                paragraph(getI18nString("accessthroughtproxy.svn.intro2"));
                paragraph(getI18nString("accessthroughtproxy.svn.intro3"));

                verbatimText(
                        "[global]" + LS + "http-proxy-host = your.proxy.name" + LS + "http-proxy-port = 3128" + LS);

                endSection();
            }
        }

        // Git

        private void gitClone(String url) {
            // in the future, git scm url should support both repository + path: at the moment, require a hack
            // to remove path added to repository
            int index = url.indexOf(".git/");
            if (index > 0) {
                url = url.substring(0, index + 4);
            }

            boolean head = (scmTag == null || scmTag.isEmpty()) || "HEAD".equals(scmTag);
            verbatimText("$ git clone " + (head ? "" : ("--branch " + scmTag + ' ')) + url);
        }

        /**
         * Create the documentation to provide an anonymous access with a <code>Git</code> SCM. For example, generate
         * the following command line:
         * <p>
         * git clone uri
         * </p>
         *
         * @param gitRepo
         */
        private void anonymousAccessGit(GitScmProviderRepository gitRepo) {
            sink.paragraph();
            linkPatternedText(getI18nString("anonymousaccess.git.intro"));
            sink.paragraph_();

            gitClone(gitRepo.getFetchUrl());
        }

        // Mercurial

        /**
         * Create the documentation to provide an anonymous access with a <code>Mercurial</code> SCM. For example,
         * generate the following command line:
         * <p>
         * hg clone uri
         * </p>
         *
         * @param hgRepo
         */
        private void anonymousAccessMercurial(HgScmProviderRepository hgRepo) {
            sink.paragraph();
            linkPatternedText(getI18nString("anonymousaccess.hg.intro"));
            sink.paragraph_();

            verbatimText("$ hg clone " + hgRepo.getURI());
        }

        // Git

        /**
         * Create the documentation to provide an developer access with a <code>Git</code> SCM. For example, generate
         * the following command line:
         * <p>
         * git clone repo
         * </p>
         *
         * @param gitRepo
         */
        private void developerAccessGit(GitScmProviderRepository gitRepo) {
            sink.paragraph();
            linkPatternedText(getI18nString("devaccess.git.intro"));
            sink.paragraph_();

            gitClone(gitRepo.getPushUrl());
        }

        // Mercurial

        /**
         * Create the documentation to provide an developer access with a <code>Mercurial</code> SCM. For example,
         * generate the following command line:
         * <p>
         * hg clone repo
         * </p>
         *
         * @param hgRepo
         */
        private void developerAccessMercurial(HgScmProviderRepository hgRepo) {
            sink.paragraph();
            linkPatternedText(getI18nString("devaccess.hg.intro"));
            sink.paragraph_();

            verbatimText("$ hg clone " + hgRepo.getURI());
        }

        // Subversion

        /**
         * Create the documentation to provide an anonymous access with a <code>Subversion</code>
         * SCM. For example, generate the following command line:
         * <p>
         * svn checkout http://svn.apache.org/repos/asf/maven/components/trunk maven
         * </p>
         *
         * @param svnRepo
         * @see <a href="http://svnbook.red-bean.com/">http://svnbook.red-bean.com/</a>
         */
        private void anonymousAccessSubversion(SvnScmProviderRepository svnRepo) {
            paragraph(getI18nString("anonymousaccess.svn.intro"));

            verbatimText("$ svn checkout " + svnRepo.getUrl() + " " + checkoutDirectoryName);
        }

        /**
         * Create the documentation to provide an developer access with a <code>Subversion</code>
         * SCM. For example, generate the following command line:
         * <p>
         * svn checkout https://svn.apache.org/repos/asf/maven/components/trunk maven
         * </p>
         * <p>
         * svn commit --username your-username -m "A message"
         * </p>
         *
         * @param svnRepo
         * @see <a href="http://svnbook.red-bean.com/">http://svnbook.red-bean.com/</a>
         */
        private void developerAccessSubversion(SvnScmProviderRepository svnRepo) {
            if (svnRepo.getUrl() != null) {
                if (svnRepo.getUrl().startsWith("https://")) {
                    paragraph(getI18nString("devaccess.svn.intro1.https"));
                } else if (svnRepo.getUrl().startsWith("svn://")) {
                    paragraph(getI18nString("devaccess.svn.intro1.svn"));
                } else if (svnRepo.getUrl().startsWith("svn+ssh://")) {
                    paragraph(getI18nString("devaccess.svn.intro1.svnssh"));
                } else {
                    paragraph(getI18nString("devaccess.svn.intro1.other"));
                }
            }

            StringBuilder sb = new StringBuilder();

            sb.append("$ svn checkout ").append(svnRepo.getUrl()).append(" ").append(checkoutDirectoryName);

            verbatimText(sb.toString());

            paragraph(getI18nString("devaccess.svn.intro2"));

            sb = new StringBuilder();
            sb.append("$ svn commit --username your-username -m \"A message\"");

            verbatimText(sb.toString());
        }

        /**
         * Return a <code>SCM repository</code> defined by a given url
         *
         * @param scmUrl an SCM URL
         * @return a valid SCM repository or null
         */
        public ScmRepository getScmRepository(String scmUrl) {
            if (scmUrl == null || scmUrl.isEmpty()) {
                return null;
            }

            ScmRepository repo = null;
            List<String> messages = new ArrayList<>();
            try {
                messages.addAll(scmManager.validateScmRepository(scmUrl));
            } catch (Exception e) {
                messages.add(e.getMessage());
            }

            if (!messages.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                boolean isIntroAdded = false;
                for (String msg : messages) {
                    // Ignore NoSuchScmProviderException msg
                    // See impl of AbstractScmManager#validateScmRepository()
                    if (msg.startsWith("No such provider")) {
                        continue;
                    }

                    if (!isIntroAdded) {
                        sb.append("This SCM url '");
                        sb.append(scmUrl);
                        sb.append("' is invalid due to the following errors:");
                        sb.append(LS);
                        isIntroAdded = true;
                    }
                    sb.append(" * ");
                    sb.append(msg);
                    sb.append(LS);
                }

                if (StringUtils.isNotEmpty(sb.toString())) {
                    sb.append("For more information about SCM URL Format, please refer to: "
                            + "http://maven.apache.org/scm/scm-url-format.html");

                    throw new IllegalArgumentException(sb.toString());
                }
            }

            try {
                repo = scmManager.makeScmRepository(scmUrl);
            } catch (Exception e) {
                // Should be already catched
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
            }

            return repo;
        }

        /**
         * Convenience method that return true is the defined <code>SCM repository</code> is a known provider.
         * <p>
         * Currently, we fully support ClearCase, CVS, Git, Perforce, Mercurial, Starteam and Subversion
         * by the maven-scm-providers component.
         * </p>
         *
         * @param scmRepository a SCM repository
         * @param scmProvider a SCM provider name
         * @return true if the provider of the given SCM repository is equal to the given scm provider.
         * @see <a href="http://svn.apache.org/repos/asf/maven/scm/trunk/maven-scm-providers/">maven-scm-providers</a>
         */
        private static boolean isScmSystem(ScmRepository scmRepository, String scmProvider) {
            if (scmProvider == null || scmProvider.isEmpty()) {
                return false;
            }
            return scmRepository != null && scmProvider.equalsIgnoreCase(scmRepository.getProvider());
        }
    }
}
