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

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

/**
 * @author Hervé Boutemy
 *
 */
public abstract class AbstractProjectInfoRenderer extends AbstractMavenReportRenderer {
    /**
     * {@link I18N}.
     */
    protected I18N i18n;

    /**
     * The {@link Locale}
     */
    protected Locale locale;

    /**
     * @param sink {@link Sink}
     * @param i18n {@link I18N}
     * @param locale {@link Locale}
     */
    public AbstractProjectInfoRenderer(Sink sink, I18N i18n, Locale locale) {
        super(sink);

        this.i18n = i18n;

        this.locale = locale;
    }

    @Override
    public String getTitle() {
        return getI18nString("title");
    }

    /**
     * @param key The key.
     * @return The translated string.
     */
    protected String getI18nString(String key) {
        return getI18nString(getI18Nsection(), key);
    }

    /**
     * @param section The section.
     * @param key The key to translate.
     * @return the translated key.
     */
    protected String getI18nString(String section, String key) {
        return i18n.getString("project-info-reports", locale, "report." + section + '.' + key);
    }

    @Override
    protected void text(String text) {
        if (text == null || text.isEmpty()) // Take care of spaces
        {
            sink.text("-");
        } else {
            // custombundle text with xml?
            String regex = "(.+?)<(\"[^\"]*\"|'[^']*'|[^'\">])*>(.+?)";
            if (Pattern.matches(regex, text)) {
                sink.rawText(text);
            } else {
                sink.text(text);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden because Maven Reporting Impl 4.0.0 calls {@link Sink#verbatim()}, which only exists since Doxia 2,
     * while a report is rendered with the Doxia of the Maven Site Plugin in use, which may still be Doxia 1. Only
     * {@link Sink#verbatim(SinkEventAttributes)} exists in both. The fix merged upstream as
     * apache/maven-reporting-impl#243 (not released yet, expected in 4.0.1) does the same, so this override can go
     * once a release containing it is used.
     */
    @Override
    protected void verbatimText(String text) {
        sink.verbatim((SinkEventAttributes) null);

        text(text);

        sink.verbatim_();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden for the same reason as {@link #verbatimText(String)}.
     */
    @Override
    protected void verbatimLink(String text, String href) {
        if (href == null || href.isEmpty()) {
            verbatimText(text);
        } else {
            sink.verbatim((SinkEventAttributes) null);

            link(href, text);

            sink.verbatim_();
        }
    }

    protected abstract String getI18Nsection();
}
