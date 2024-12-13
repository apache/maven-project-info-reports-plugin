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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.codehaus.plexus.i18n.I18N;

/**
 * Generates the Project Continuous Integration Management report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @since 2.0
 */
@Mojo(name = "ci-management")
public class CiManagementReport extends AbstractProjectInfoReport {

    @Inject
    public CiManagementReport(
            ArtifactResolver resolver, RepositorySystem repositorySystem, I18N i18n, ProjectBuilder projectBuilder) {
        super(resolver, repositorySystem, i18n, projectBuilder);
    }
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        boolean result = super.canGenerateReport();
        if (result && skipEmptyReport) {
            CiManagement cim = getProject().getModel().getCiManagement();
            result = cim != null;
        }

        return result;
    }

    @Override
    public void executeReport(Locale locale) {
        CiManagementRenderer r =
                new CiManagementRenderer(getSink(), getProject().getModel(), getI18N(locale), locale);

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName() {
        return "ci-management";
    }

    @Override
    protected String getI18Nsection() {
        return "ci-management";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class CiManagementRenderer extends AbstractProjectInfoRenderer {

        private static final Set<String> SYSTEMS = new HashSet<>(Arrays.asList(
                "anthill",
                "bamboo",
                "buildforge",
                "continuum",
                "cruisecontrol",
                "github",
                "hudson",
                "jenkins",
                "luntbuild",
                "teamcity",
                "travis"));

        private Model model;

        CiManagementRenderer(Sink sink, Model model, I18N i18n, Locale locale) {
            super(sink, i18n, locale);

            this.model = model;
        }

        @Override
        protected String getI18Nsection() {
            return "ci-management";
        }

        @Override
        protected void renderBody() {
            CiManagement cim = model.getCiManagement();
            if (cim == null) {
                startSection(getTitle());

                paragraph(getI18nString("nocim"));

                endSection();

                return;
            }

            String system = cim.getSystem();
            String url = cim.getUrl();
            List<Notifier> notifiers = cim.getNotifiers();

            // Overview
            startSection(getI18nString("overview.title"));

            sink.paragraph();
            linkPatternedText(getIntroForCiManagementSystem(system));
            sink.paragraph_();

            endSection();

            // Access
            startSection(getI18nString("access"));

            if (!(url == null || url.isEmpty())) {
                paragraph(getI18nString("url"));

                verbatimLink(url, url);
            } else {
                paragraph(getI18nString("nourl"));
            }

            endSection();

            // Notifiers
            startSection(getI18nString("notifiers.title"));

            if (notifiers == null || notifiers.isEmpty()) {
                paragraph(getI18nString("notifiers.nolist"));
            } else {
                sink.paragraph();
                sink.text(getI18nString("notifiers.intro"));
                sink.paragraph_();

                startTable();

                String type = getI18nString("notifiers.column.type");
                String address = getI18nString("notifiers.column.address");
                String configuration = getI18nString("notifiers.column.configuration");

                tableHeader(new String[] {type, address, configuration});

                for (Notifier notifier : notifiers) {
                    tableRow(new String[] {
                        notifier.getType(),
                        createLinkPatternedText(notifier.getAddress(), notifier.getAddress()),
                        propertiesToString(notifier.getConfiguration())
                    });
                }

                endTable();
            }

            endSection();
        }

        /**
         * Search system description.
         *
         * @param system a system for description
         * @return system description from properties
         */
        private String getIntroForCiManagementSystem(String system) {
            if (system == null || system.isEmpty()) {
                return getI18nString("general.intro");
            }

            String systemLowerCase = system.toLowerCase(Locale.ENGLISH);

            for (String systemName : SYSTEMS) {
                if (systemLowerCase.startsWith(systemName)) {
                    return getI18nString(systemName + ".intro");
                }
            }

            return getI18nString("general.intro");
        }
    }
}
