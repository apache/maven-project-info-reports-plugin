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
<td style="text-align: left;">module-info-only-test-0.0.1.jar</td>
<td style="text-align: right;">3.3 kB</td>
<td style="text-align: right;">10</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">-</td>
<td style="text-align: center;">-</td>
<td style="text-align: center;">-</td></tr>
<tr class="a">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Root</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">8</td>
<td style="text-align: right;">0</td>
<td style="text-align: right;">0</td>
<td style="text-align: center;">-</td>
<td style="text-align: center;">-</td></tr>
<tr class="b">
<td style="text-align: left;">&#160;&#160;&#160;&#x2022; Versioned</td>
<td style="text-align: right;">-</td>
<td style="text-align: right;">2</td>
<td style="text-align: right;">1</td>
<td style="text-align: right;">1</td>
<td style="text-align: center;">11</td>
<td style="text-align: center;">No</td></tr>
<tr class="a">
<td style="text-align: left;">multi-release-test-0.0.1.jar</td>
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
<td style="text-align: center;">Yes</td></tr>'''

assert html.contains( summaryLine.normalize() )
