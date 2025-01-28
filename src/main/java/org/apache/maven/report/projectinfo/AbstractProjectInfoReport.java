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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Base class with the things that should be in AbstractMavenReport anyway.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @since 2.0
 */
public abstract class AbstractProjectInfoReport extends AbstractMavenReport {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * Plugin repositories used for the project.
     *
     * @since 3.1.0
     */
    @Parameter(defaultValue = "${project.pluginArtifactRepositories}", readonly = true, required = true)
    protected List<ArtifactRepository> pluginRepositories;

    /**
     * The current user system settings for use in Maven.
     *
     * @since 2.3
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    /**
     * Path for a custom bundle instead of using the default one. <br>
     * Using this field, you could change the texts in the generated reports.
     *
     * @since 2.3
     */
    @Parameter(defaultValue = "${project.basedir}/src/site/custom/project-info-reports.properties")
    protected String customBundle;

    /**
     * Skip report.
     *
     * @since 2.8
     */
    @Parameter(property = "mpir.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Skip the project info report generation if a report-specific section of the POM is empty. Defaults to
     * <code>true</code>.
     *
     * @since 2.8
     */
    @Parameter(defaultValue = "true")
    protected boolean skipEmptyReport;

    /**
     * A mapping of license names to group licenses referred to with different names together
     *
     * @since 3.3.1
     */
    @Parameter
    private List<LicenseMapping> licenseMappings;

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Artifact Factory component.
     */
    final RepositorySystem repositorySystem;

    /**
     * Internationalization component, could support also custom bundle using {@link #customBundle}.
     */
    private I18N i18n;

    protected final ProjectBuilder projectBuilder;

    protected AbstractProjectInfoReport(RepositorySystem repositorySystem, I18N i18n, ProjectBuilder projectBuilder) {
        this.repositorySystem = repositorySystem;
        this.i18n = i18n;
        this.projectBuilder = projectBuilder;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        return !skip;
    }

    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_INFORMATION;
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    protected Map<String, String> getLicenseMappings() {
        Map<String, String> map = new HashMap<>();
        if (licenseMappings != null) {
            for (LicenseMapping mapping : licenseMappings) {
                for (String from : mapping.getFroms()) {
                    map.put(from, mapping.getTo());
                }
            }
        }
        return map;
    }

    /**
     * @param coll The collection to be checked.
     * @return true if coll is empty false otherwise.
     */
    protected boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    protected MavenSession getSession() {
        return session;
    }

    protected List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    protected MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    /**
     * @param pluginId The id of the plugin
     * @return The information about the plugin
     */
    protected Plugin getPlugin(String pluginId) {
        if ((getProject().getBuild() == null) || (getProject().getBuild().getPluginsAsMap() == null)) {
            return null;
        }

        Plugin plugin = getProject().getBuild().getPluginsAsMap().get(pluginId);

        if ((plugin == null)
                && (getProject().getBuild().getPluginManagement() != null)
                && (getProject().getBuild().getPluginManagement().getPluginsAsMap() != null)) {
            plugin = getProject()
                    .getBuild()
                    .getPluginManagement()
                    .getPluginsAsMap()
                    .get(pluginId);
        }

        return plugin;
    }

    /**
     * @param pluginId the pluginId
     * @param param the child which should be checked
     * @return the value of the dom tree
     */
    protected String getPluginParameter(String pluginId, String param) {
        Plugin plugin = getPlugin(pluginId);
        if (plugin != null) {
            Xpp3Dom xpp3Dom = (Xpp3Dom) plugin.getConfiguration();
            if (xpp3Dom != null
                    && xpp3Dom.getChild(param) != null
                    && StringUtils.isNotEmpty(xpp3Dom.getChild(param).getValue())) {
                return xpp3Dom.getChild(param).getValue();
            }
        }

        return null;
    }

    /**
     * @param locale The locale
     * @param key The key to search for
     * @return The text appropriate for the locale.
     */
    protected String getI18nString(Locale locale, String key) {
        return getI18N(locale).getString("project-info-reports", locale, "report." + getI18Nsection() + '.' + key);
    }

    /**
     * @param locale The local.
     * @return I18N for the locale
     */
    protected I18N getI18N(Locale locale) {
        if (customBundle != null) {
            File customBundleFile = new File(customBundle);
            if (customBundleFile.isFile() && customBundleFile.getName().endsWith(".properties")) {
                if (!i18n.getClass().isAssignableFrom(CustomI18N.class)
                        || !i18n.getDefaultLanguage().equals(locale.getLanguage())) {
                    // first load
                    i18n = new CustomI18N(project, settings, customBundleFile, locale, i18n);
                }
            }
        }

        return i18n;
    }

    /**
     * @return The according string for the section.
     */
    protected abstract String getI18Nsection();

    /** {@inheritDoc} */
    public String getName(Locale locale) {
        return getI18nString(locale, "name");
    }

    /** {@inheritDoc} */
    public String getDescription(Locale locale) {
        return getI18nString(locale, "description");
    }

    // TODO Review, especially Locale.getDefault()
    private static class CustomI18N implements I18N {
        private final MavenProject project;

        private final Settings settings;

        private final String bundleName;

        private final Locale locale;

        private final I18N i18nOriginal;

        private ResourceBundle bundle;

        private static final Object[] NO_ARGS = new Object[0];

        CustomI18N(MavenProject project, Settings settings, File customBundleFile, Locale locale, I18N i18nOriginal) {
            super();
            this.project = project;
            this.settings = settings;
            this.locale = locale;
            this.i18nOriginal = i18nOriginal;
            this.bundleName = customBundleFile
                    .getName()
                    .substring(0, customBundleFile.getName().indexOf(".properties"));

            URLClassLoader classLoader = null;
            try {
                classLoader = new URLClassLoader(
                        new URL[] {customBundleFile.getParentFile().toURI().toURL()}, null);
            } catch (MalformedURLException e) {
                // could not happen.
            }

            this.bundle = ResourceBundle.getBundle(this.bundleName, locale, classLoader);
            if (!this.bundle.getLocale().getLanguage().equals(locale.getLanguage())) {
                this.bundle = ResourceBundle.getBundle(this.bundleName, Locale.getDefault(), classLoader);
            }
        }

        /** {@inheritDoc} */
        public String getDefaultLanguage() {
            return locale.getLanguage();
        }

        /** {@inheritDoc} */
        public String getDefaultCountry() {
            return locale.getCountry();
        }

        /** {@inheritDoc} */
        public String getDefaultBundleName() {
            return bundleName;
        }

        /** {@inheritDoc} */
        public String[] getBundleNames() {
            return new String[] {bundleName};
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle() {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle(String bundleName) {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle(String bundleName, String languageHeader) {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle(String bundleName, Locale locale) {
            return bundle;
        }

        /** {@inheritDoc} */
        public Locale getLocale(String languageHeader) {
            return new Locale(languageHeader);
        }

        /** {@inheritDoc} */
        public String getString(String key) {
            return getString(bundleName, locale, key);
        }

        /** {@inheritDoc} */
        public String getString(String key, Locale locale) {
            return getString(bundleName, locale, key);
        }

        /** {@inheritDoc} */
        public String getString(String bundleName, Locale locale, String key) {
            String value;

            if (locale == null) {
                locale = getLocale(null);
            }

            ResourceBundle rb = getBundle(bundleName, locale);
            value = getStringOrNull(rb, key);

            if (value == null) {
                // try to load default
                value = i18nOriginal.getString(bundleName, locale, key);
            }

            if (!value.contains("${")) {
                return value;
            }

            final RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
            try {
                interpolator.addValueSource(new EnvarBasedValueSource());
            } catch (final IOException e) {
                // In which cases could this happen? And what should we do?
            }

            interpolator.addValueSource(new PropertiesBasedValueSource(System.getProperties()));
            interpolator.addValueSource(new PropertiesBasedValueSource(project.getProperties()));
            interpolator.addValueSource(new PrefixedObjectValueSource("project", project));
            interpolator.addValueSource(new PrefixedObjectValueSource("pom", project));
            interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));

            try {
                value = interpolator.interpolate(value);
            } catch (final InterpolationException e) {
                // What does this exception mean?
            }

            return value;
        }

        /** {@inheritDoc} */
        public String format(String key, Object arg1) {
            return format(bundleName, locale, key, new Object[] {arg1});
        }

        /** {@inheritDoc} */
        public String format(String key, Object arg1, Object arg2) {
            return format(bundleName, locale, key, new Object[] {arg1, arg2});
        }

        /** {@inheritDoc} */
        public String format(String bundleName, Locale locale, String key, Object arg1) {
            return format(bundleName, locale, key, new Object[] {arg1});
        }

        /** {@inheritDoc} */
        public String format(String bundleName, Locale locale, String key, Object arg1, Object arg2) {
            return format(bundleName, locale, key, new Object[] {arg1, arg2});
        }

        /** {@inheritDoc} */
        public String format(String bundleName, Locale locale, String key, Object[] args) {
            if (locale == null) {
                locale = getLocale(null);
            }

            String value = getString(bundleName, locale, key);
            if (args == null) {
                args = NO_ARGS;
            }

            MessageFormat messageFormat = new MessageFormat("");
            messageFormat.setLocale(locale);
            messageFormat.applyPattern(value);

            return messageFormat.format(args);
        }

        private String getStringOrNull(ResourceBundle rb, String key) {
            if (rb != null) {
                try {
                    return rb.getString(key);
                } catch (MissingResourceException ignored) {
                    // intentional
                }
            }
            return null;
        }
    }
}
