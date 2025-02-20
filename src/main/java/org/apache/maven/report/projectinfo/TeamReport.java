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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.report.projectinfo.avatars.AvatarsProvider;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the Project Team report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @since 2.0
 */
@Mojo(name = "team")
public class TeamReport extends AbstractProjectInfoReport {
    /**
     * Shows avatar images for team members that have a) properties/picUrl set b) An avatar at gravatar.com for their
     * email address
     *
     * @since 2.6
     */
    @Parameter(property = "teamlist.showAvatarImages", defaultValue = "true")
    private boolean showAvatarImages;

    /**
     * Indicate if URL should be used for avatar images.
     * <p>
     * If set to <code>false</code> images will be downloaded and attached to report during build.
     * Local path will be used for images.
     *
     * @since 3.9.0
     */
    @Parameter(property = "teamlist.externalAvatarImages", defaultValue = "true")
    private boolean externalAvatarImages;

    /**
     * Base URL for avatar provider.
     *
     * @since 3.9.0
     */
    @Parameter(property = "teamlist.avatarBaseUrl", defaultValue = "https://www.gravatar.com/avatar/")
    private String avatarBaseUrl;

    /**
     * Provider name for avatar images.
     * <p>
     * Report has one implementation for gravatar.com. Users can provide other by implementing {@link AvatarsProvider}.
     *
     * @since 3.9.0
     */
    @Parameter(property = "teamlist.avatarProviderName", defaultValue = "gravatar")
    private String avatarProviderName;

    private final Map<String, AvatarsProvider> avatarsProviders;

    @Inject
    public TeamReport(
            RepositorySystem repositorySystem,
            I18N i18n,
            ProjectBuilder projectBuilder,
            Map<String, AvatarsProvider> avatarsProviders) {
        super(repositorySystem, i18n, projectBuilder);
        this.avatarsProviders = avatarsProviders;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        boolean result = super.canGenerateReport();
        if (result && skipEmptyReport) {
            result = !isEmpty(getProject().getModel().getDevelopers())
                    || !isEmpty(getProject().getModel().getContributors());
        }

        return result;
    }

    @Override
    public void executeReport(Locale locale) throws MavenReportException {

        Map<Contributor, String> avatarImages = prepareAvatars();

        ProjectTeamRenderer renderer =
                new ProjectTeamRenderer(getSink(), project, getI18N(locale), locale, showAvatarImages, avatarImages);
        renderer.render();
    }

    private Map<Contributor, String> prepareAvatars() throws MavenReportException {

        if (!showAvatarImages) {
            return Collections.emptyMap();
        }

        AvatarsProvider avatarsProvider = avatarsProviders.get(avatarProviderName);
        if (avatarsProvider == null) {
            throw new MavenReportException("No AvatarsProvider found for name " + avatarProviderName);
        }
        avatarsProvider.setBaseUrl(avatarBaseUrl);
        avatarsProvider.setOutputDirectory(getReportOutputDirectory());

        Map<Contributor, String> result = new HashMap<>();
        try {
            prepareContributorAvatars(result, avatarsProvider, project.getDevelopers());
            prepareContributorAvatars(result, avatarsProvider, project.getContributors());
        } catch (IOException e) {
            throw new MavenReportException("Unable to load avatar images", e);
        }
        return result;
    }

    private void prepareContributorAvatars(
            Map<Contributor, String> avatarImages,
            AvatarsProvider avatarsProvider,
            List<? extends Contributor> contributors)
            throws IOException {

        for (Contributor contributor : contributors) {

            String picSource = contributor.getProperties().getProperty("picUrl");
            if (picSource == null || picSource.isEmpty()) {
                picSource = externalAvatarImages
                        ? avatarsProvider.getAvatarUrl(contributor.getEmail())
                        : avatarsProvider.getLocalAvatarPath(contributor.getEmail());
            }

            avatarImages.put(contributor, picSource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName() {
        return "team";
    }

    @Override
    protected String getI18Nsection() {
        return "team";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class ProjectTeamRenderer extends AbstractProjectInfoRenderer {
        private static final String PROPERTIES = "properties";

        private static final String TIME_ZONE = "timeZone";

        private static final String ROLES = "roles";

        private static final String ORGANIZATION_URL = "organizationUrl";

        private static final String ORGANIZATION = "organization";

        private static final String URL = "url";

        private static final String EMAIL = "email";

        private static final String NAME = "name";

        private static final String IMAGE = "image";

        private static final String ID = "id";

        private final MavenProject mavenProject;

        private final boolean showAvatarImages;

        private final Map<Contributor, String> avatarImages;

        ProjectTeamRenderer(
                Sink sink,
                MavenProject mavenProject,
                I18N i18n,
                Locale locale,
                boolean showAvatarImages,
                Map<Contributor, String> avatarImages) {
            super(sink, i18n, locale);

            this.mavenProject = mavenProject;
            this.showAvatarImages = showAvatarImages;
            this.avatarImages = avatarImages;
        }

        @Override
        protected String getI18Nsection() {
            return "team";
        }

        @Override
        protected void renderBody() {
            startSection(getI18nString("intro.title"));

            // Introduction
            paragraph(getI18nString("intro.description1"));
            paragraph(getI18nString("intro.description2"));

            // Developer section
            List<Developer> developers = mavenProject.getDevelopers();

            startSection(getI18nString("developers.title"));

            if (developers.isEmpty()) {
                paragraph(getI18nString("nodeveloper"));
            } else {
                paragraph(getI18nString("developers.intro"));

                startTable();

                // By default we think that all headers not required: set true for headers that are required
                Map<String, Boolean> headersMap = checkRequiredHeaders(developers);
                String[] requiredHeaders = getRequiredDevHeaderArray(headersMap);

                tableHeader(requiredHeaders);

                for (Developer developer : developers) {
                    renderTeamMember(developer, headersMap);
                }

                endTable();
            }

            endSection();

            // contributors section
            List<Contributor> contributors = mavenProject.getContributors();

            startSection(getI18nString("contributors.title"));

            if (contributors.isEmpty()) {
                paragraph(getI18nString("nocontributor"));
            } else {
                paragraph(getI18nString("contributors.intro"));

                startTable();

                Map<String, Boolean> headersMap = checkRequiredHeaders(contributors);
                String[] requiredHeaders = getRequiredContrHeaderArray(headersMap);

                tableHeader(requiredHeaders);

                for (Contributor contributor : contributors) {
                    renderTeamMember(contributor, headersMap);
                }

                endTable();
            }

            endSection();

            endSection();
        }

        private void renderTeamMember(Contributor member, Map<String, Boolean> headersMap) {
            sink.tableRow();

            if (headersMap.get(IMAGE) == Boolean.TRUE && showAvatarImages) {
                sink.tableCell();
                sink.figure();
                sink.figureGraphics(avatarImages.get(member));
                sink.figure_();
                sink.tableCell_();
            }
            if (member instanceof Developer) {
                if (headersMap.get(ID) == Boolean.TRUE) {
                    String id = ((Developer) member).getId();
                    if (id == null) {
                        tableCell(null);
                    } else {
                        tableCell("<a id=\"" + id + "\"></a>" + id, true);
                    }
                }
            }
            if (headersMap.get(NAME) == Boolean.TRUE) {
                tableCell(member.getName());
            }
            if (headersMap.get(EMAIL) == Boolean.TRUE) {
                final String link = String.format("mailto:%s", member.getEmail());
                tableCell(createLinkPatternedText(member.getEmail(), link));
            }
            if (headersMap.get(URL) == Boolean.TRUE) {
                tableCellForUrl(member.getUrl());
            }
            if (headersMap.get(ORGANIZATION) == Boolean.TRUE) {
                tableCell(member.getOrganization());
            }
            if (headersMap.get(ORGANIZATION_URL) == Boolean.TRUE) {
                tableCellForUrl(member.getOrganizationUrl());
            }
            if (headersMap.get(ROLES) == Boolean.TRUE) {
                if (member.getRoles() != null) {
                    // Comma separated roles
                    List<String> var = member.getRoles();
                    tableCell(StringUtils.join(var.toArray(new String[var.size()]), ", "));
                } else {
                    tableCell(null);
                }
            }
            if (headersMap.get(TIME_ZONE) == Boolean.TRUE) {
                tableCell(member.getTimezone());
            }

            if (headersMap.get(PROPERTIES) == Boolean.TRUE) {
                Properties props = member.getProperties();
                if (props != null) {
                    tableCell(propertiesToString(props));
                } else {
                    tableCell(null);
                }
            }

            sink.tableRow_();
        }

        private String[] getRequiredContrHeaderArray(Map<String, Boolean> requiredHeaders) {
            List<String> requiredArray = new ArrayList<>();
            String image = getI18nString("contributors.image");
            String name = getI18nString("contributors.name");
            String email = getI18nString("contributors.email");
            String url = getI18nString("contributors.url");
            String organization = getI18nString("contributors.organization");
            String organizationUrl = getI18nString("contributors.organizationurl");
            String roles = getI18nString("contributors.roles");
            String timeZone = getI18nString("contributors.timezone");
            String properties = getI18nString("contributors.properties");
            if (requiredHeaders.get(IMAGE) == Boolean.TRUE && showAvatarImages) {
                requiredArray.add(image);
            }
            setRequiredArray(
                    requiredHeaders,
                    requiredArray,
                    name,
                    email,
                    url,
                    organization,
                    organizationUrl,
                    roles,
                    timeZone,
                    properties);

            return requiredArray.toArray(new String[requiredArray.size()]);
        }

        private String[] getRequiredDevHeaderArray(Map<String, Boolean> requiredHeaders) {
            List<String> requiredArray = new ArrayList<>();

            String image = getI18nString("developers.image");
            String id = getI18nString("developers.id");
            String name = getI18nString("developers.name");
            String email = getI18nString("developers.email");
            String url = getI18nString("developers.url");
            String organization = getI18nString("developers.organization");
            String organizationUrl = getI18nString("developers.organizationurl");
            String roles = getI18nString("developers.roles");
            String timeZone = getI18nString("developers.timezone");
            String properties = getI18nString("developers.properties");

            if (requiredHeaders.get(IMAGE) == Boolean.TRUE && showAvatarImages) {
                requiredArray.add(image);
            }
            if (requiredHeaders.get(ID) == Boolean.TRUE) {
                requiredArray.add(id);
            }

            setRequiredArray(
                    requiredHeaders,
                    requiredArray,
                    name,
                    email,
                    url,
                    organization,
                    organizationUrl,
                    roles,
                    timeZone,
                    properties);

            return requiredArray.toArray(new String[0]);
        }

        private static void setRequiredArray(
                Map<String, Boolean> requiredHeaders,
                List<String> requiredArray,
                String name,
                String email,
                String url,
                String organization,
                String organizationUrl,
                String roles,
                String timeZone,
                String properties) {
            if (requiredHeaders.get(NAME) == Boolean.TRUE) {
                requiredArray.add(name);
            }
            if (requiredHeaders.get(EMAIL) == Boolean.TRUE) {
                requiredArray.add(email);
            }
            if (requiredHeaders.get(URL) == Boolean.TRUE) {
                requiredArray.add(url);
            }
            if (requiredHeaders.get(ORGANIZATION) == Boolean.TRUE) {
                requiredArray.add(organization);
            }
            if (requiredHeaders.get(ORGANIZATION_URL) == Boolean.TRUE) {
                requiredArray.add(organizationUrl);
            }
            if (requiredHeaders.get(ROLES) == Boolean.TRUE) {
                requiredArray.add(roles);
            }
            if (requiredHeaders.get(TIME_ZONE) == Boolean.TRUE) {
                requiredArray.add(timeZone);
            }

            if (requiredHeaders.get(PROPERTIES) == Boolean.TRUE) {
                requiredArray.add(properties);
            }
        }

        /**
         * @param units contributors and developers to check
         * @return required headers
         */
        private static Map<String, Boolean> checkRequiredHeaders(List<? extends Contributor> units) {
            Map<String, Boolean> requiredHeaders = new HashMap<>();

            requiredHeaders.put(IMAGE, Boolean.FALSE);
            requiredHeaders.put(ID, Boolean.FALSE);
            requiredHeaders.put(NAME, Boolean.FALSE);
            requiredHeaders.put(EMAIL, Boolean.FALSE);
            requiredHeaders.put(URL, Boolean.FALSE);
            requiredHeaders.put(ORGANIZATION, Boolean.FALSE);
            requiredHeaders.put(ORGANIZATION_URL, Boolean.FALSE);
            requiredHeaders.put(ROLES, Boolean.FALSE);
            requiredHeaders.put(TIME_ZONE, Boolean.FALSE);
            requiredHeaders.put(PROPERTIES, Boolean.FALSE);

            for (Contributor unit : units) {
                if (unit instanceof Developer) {
                    Developer developer = (Developer) unit;
                    if (StringUtils.isNotEmpty(developer.getId())) {
                        requiredHeaders.put(ID, Boolean.TRUE);
                    }
                }
                if (StringUtils.isNotEmpty(unit.getName())) {
                    requiredHeaders.put(NAME, Boolean.TRUE);
                }
                if (StringUtils.isNotEmpty(unit.getEmail())) {
                    requiredHeaders.put(EMAIL, Boolean.TRUE);
                    requiredHeaders.put(IMAGE, Boolean.TRUE);
                }
                if (StringUtils.isNotEmpty(unit.getUrl())) {
                    requiredHeaders.put(URL, Boolean.TRUE);
                }
                if (StringUtils.isNotEmpty(unit.getOrganization())) {
                    requiredHeaders.put(ORGANIZATION, Boolean.TRUE);
                }
                if (StringUtils.isNotEmpty(unit.getOrganizationUrl())) {
                    requiredHeaders.put(ORGANIZATION_URL, Boolean.TRUE);
                }
                if (!unit.getRoles().isEmpty()) {
                    requiredHeaders.put(ROLES, Boolean.TRUE);
                }
                if (StringUtils.isNotEmpty(unit.getTimezone())) {
                    requiredHeaders.put(TIME_ZONE, Boolean.TRUE);
                }
                Properties properties = unit.getProperties();
                boolean hasPicUrl = properties.containsKey("picUrl");
                if (hasPicUrl) {
                    requiredHeaders.put(IMAGE, Boolean.TRUE);
                }
                boolean isJustAnImageProperty = properties.size() == 1 && hasPicUrl;
                if (!isJustAnImageProperty && !properties.isEmpty()) {
                    requiredHeaders.put(PROPERTIES, Boolean.TRUE);
                }
            }
            return requiredHeaders;
        }

        /**
         * Create a table cell with a link to the given url. The url is not validated.
         */
        private void tableCellForUrl(String url) {
            sink.tableCell();

            if (url == null || url.isEmpty()) {
                text(url);
            } else {
                link(url, url);
            }

            sink.tableCell_();
        }
    }
}
