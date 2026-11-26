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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AbstractProjectInfoRendererTest {

    /**
     * A report is rendered with the Doxia of the Maven Site Plugin in use, which may still be Doxia 1, and
     * {@link Sink#verbatim()} only exists since Doxia 2: verbatim blocks have to be opened with the overload that
     * both have.
     */
    @Test
    void verbatimBlocksUseTheOverloadDoxia1HasToo() {
        Sink sink = mock(Sink.class);
        AbstractProjectInfoRenderer renderer = new AbstractProjectInfoRenderer(sink, null, Locale.ROOT) {
            @Override
            public String getTitle() {
                return "title";
            }

            @Override
            protected String getI18Nsection() {
                return "test";
            }

            @Override
            protected void renderBody() {
                verbatimText("text");
                verbatimLink("text", "https://example.org/");
            }
        };

        renderer.render();

        verify(sink, times(2)).verbatim((SinkEventAttributes) null);
        verify(sink, never()).verbatim();
    }
}
