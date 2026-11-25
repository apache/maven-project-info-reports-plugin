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

File siteDir = new File(basedir, "target/site")
assert siteDir.isDirectory() : "Site directory not existent: " + siteDir
String[] reports = [
    "index",
    // <report>index</report>
    "summary",
    // <report>summary</report>
    "licenses",
    // <report>licenses</report>
    "team",
    // <report>team</report>
    "scm",
    // <report>scm</report>
    "issue-management",
    // <report>issue-management</report>
    "mailing-lists",
    // <report>mailing-lists</report>
    "dependency-info",
    // <report>dependency-info</report>
    "dependency-management",
    // <report>dependency-management</report>
    "dependencies",
    // <report>dependencies</report>
    "dependency-convergence",
    // <report>dependency-convergence</report>
    "ci-management",
    // <report>ci-management</report>
    "plugin-management",
    // <report>plugin-management</report>
    "plugins",
    // <report>plugins</report>
    "distribution-management" // <report>distribution-management</report>
] as String[]

String info = new File( siteDir, "project-info.html" ).getText( 'UTF-8' )

int index1 = 10
int index2 = 10
String previousReportLink

// MPIR-182: verify order of reports
for ( String report : reports ) {
    File reportFile = new File( siteDir, report + ".html" )
    assert reportFile.isFile() : "Report file not existent: " + reportFile

    String link = "<a href=\"" + reportFile.getName() + "\""
    int i1 = info.indexOf( link )
    int i2 = info.indexOf( link, i1 + 1 )

    assert i1 >= index1 : "Wrong order for first report link: expected " + previousReportLink + " -> " + link + ", but found " + i1 + " < " + index1 +
    "\n   previous report link: " + info.substring( index1 - 10, index1 + 70 ) +
    "\n     actual report link: " + info.substring( i1 - 10, i1 + 70 )
    assert i2 >= index2 : "Wrong order for second report link: expected " + previousReportLink + " -> " + link + ", but found " + i2 + " < " + index2 +
    "\n   previous report link: " + info.substring( index2 - 10, index2 + 70 ) +
    "\n     actual report link: " + info.substring( i2 - 10, i2 + 70 )

    index1 = i1
    index2 = i2
    previousReportLink = link
}

File mailinglists = new File( siteDir, "mailing-lists.html")
String content = mailinglists.getText( 'UTF-8' )
assert content.contains( "mail list intro text foo" ) : "mailing-lists.html doesn't contain mail list intro text foo"

mailinglists = new File( siteDir, "de/mailing-lists.html")
content = mailinglists.getText( 'UTF-8' )
assert content.contains( "mail list intro text foo (&quot;de&quot;)" ) : "de/mailing-lists.html doesn't contain mail list intro text foo (\"de\")"

mailinglists = new File( siteDir, "fr/mailing-lists.html")
content = mailinglists.getText( 'UTF-8' )
assert content.contains( "mail list intro text foo (&quot;fr&quot;)" ) : "fr/mailing-lists.html doesn't contain mail list intro text foo (\"fr\")"

mailinglists = new File( siteDir, "sv/mailing-lists.html")
content = mailinglists.getText( 'UTF-8' )
assert content.contains( "mail list intro text foo" ) : "sv/mailing-lists.html doesn't contain mail list intro text foo"

File dependencies = new File( siteDir, "dependencies.html")
content = dependencies.getText( 'UTF-8' )
assert content.contains( "doxia-core-1.2.jar" ) : "dependencies.html doesn't contain doxia-core-1.2.jar"

// MPIR #687: tree details use <details>/<summary>, no images or JavaScript
assert content.contains( "<details><summary>" ) && content.contains( "</details>" ) : "dependencies.html doesn't contain <details><summary> for the dependency tree"
assert !( content.contains( "toggleDependencyDetails" ) || content.contains( "icon_info_sml" ) || content.contains( "close.gif" ) ) : "dependencies.html still uses images or JavaScript for dependency details"

// MPIR-216: dependency with range
File dependencyMgmt = new File( siteDir, "dependency-management.html")
content = dependencyMgmt.getText( 'UTF-8' )
assert content.contains( "https://maven.apache.org/doxia/doxia/doxia-sink-api/" ) : "MPIR-216: dependency-management doesn't contain doxia-sink-api url https://maven.apache.org/doxia/doxia/doxia-sink-api/"

File dependencyConvergence = new File( siteDir, "dependency-convergence.html")
content = dependencyConvergence.getText( 'UTF-8' )

assert content.contains( "You do not have 100% convergence." ) : "dependency-convergence not rendered correctly"

// MPIR #687: convergence marks are text, not images
assert content.contains( "<td>&#x2718; " ) : "dependency-convergence doesn't contain the text error mark"
assert !( content.contains( "icon_success_sml" ) || content.contains( "icon_error_sml" ) ) : "dependency-convergence still uses icon images"
