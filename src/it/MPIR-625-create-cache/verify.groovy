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
String html = new File( basedir, 'target/reports/dependencies.html' ).text.normalize()

def summaryLine = '''\
<tr class="b">
<td style="text-align: left;">commons-math3-3.6.1-tools.jar</td>
<td style="text-align: right;">12 kB</td>
<td style="text-align: right;">10</td>
<td style="text-align: right;">2</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">1.5</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="a">
<td style="text-align: left;">snapshot-test-1.0-SNAPSHOT.jar</td>
<td style="text-align: right;">10.2 kB</td>
<td style="text-align: right;">37</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">-</td>
<td style="text-align: center;">-</td>
<td style="text-align: center;">-</td></tr>
<tr class="b">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Root</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">17</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">1.8</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="a">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Versioned</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">10</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">9</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="b">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Versioned</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">10</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">11</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="a">
<td style="text-align: left;">plexus-utils-4.0.0.jar</td>
<td style="text-align: right;">192.4 kB</td>
<td style="text-align: right;">128</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">-</td>
<td style="text-align: center;">-</td>
<td style="text-align: center;">-</td></tr>
<tr class="b">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Root</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">110</td>
<td style="text-align: right;">86</td>
<td style="text-align: right;">7</td>
<td style="text-align: center;">1.8</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="a">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Versioned</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">6</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">9</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="b">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Versioned</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">6</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">10</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="a">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Versioned</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">6</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">11</td>
<td style="text-align: center;">Yes</td></tr>
<tr class="b">'''

assert html.contains( summaryLine.normalize() )

File log = new File( basedir, 'build.log' );
String logContent = log.text;
assert logContent.contains('JarDataSummary analyzed for: org.apache.commons:commons-math3:jar:tools:3.6.1:compile');
assert logContent.contains('JarDataSummary analyzed for: org.apache.maven.its.mpir-465:snapshot-test:jar:1.0-SNAPSHOT:compile')
assert logContent.contains('JarDataSummary analyzed for: org.codehaus.plexus:plexus-utils:jar:4.0.0:compile')

assert !logContent.contains('JarDataSummary cached for:')
