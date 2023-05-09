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

    /* FIXME The next two methods need to be retained until Doxia and Maven Reporting Impl properly implement
     * the difference of a (boxed) real verbatim text and (boxed) source code.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    protected void verbatimText(String text) {
        sink.verbatim(null);

        text(text);

        sink.verbatim_();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void verbatimLink(String text, String href) {
        if (href == null || href.isEmpty()) {
            verbatimText(text);
        } else {
            sink.verbatim(null);

            link(href, text);

            sink.verbatim_();
        }
    }

    protected abstract String getI18Nsection();
}
