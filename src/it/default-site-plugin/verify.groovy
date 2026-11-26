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

def buildLog = new File( basedir, 'build.log' ).getText( 'UTF-8' )

// The Maven Site Plugin skips a report that fails with a LinkageError: the page is cut short and the build
// still ends in BUILD SUCCESS, so the build result says nothing and the log has to be searched.
def skipped = buildLog.readLines().findAll { it.contains( 'LinkageError' ) }
assert skipped.isEmpty() : 'Reports cut short:\n' + skipped.join( '\n' )

// With no version declared, Maven 3.9.x binds Maven Site Plugin 3.12.1, which provides Doxia 1. Should that
// default change, this project would silently stop covering Doxia 1.
if ( mavenVersion.startsWith( '3.9.' ) )
{
    assert buildLog.contains( ':3.12.1:site' ) :
        'Maven no longer binds Maven Site Plugin 3.12.1 by default: declare it in the POM to keep covering Doxia 1'
}

def report = { String name -> new File( basedir, "target/site/${name}.html" ).getText( 'UTF-8' ) }

// Each of these reports used to stop before the text looked for here: at its first verbatim block...
assert report( 'ci-management' ).contains( 'https://example.org/ci/default-site-plugin' )
assert report( 'dependency-info' ).contains( 'Leiningen' )
assert report( 'issue-management' ).contains( 'https://example.org/issues/default-site-plugin' )
assert report( 'licenses' ).contains( 'This is the license text of the default-site-plugin integration test project.' )
assert report( 'scm' ).contains( '$ git clone https://example.org/git/default-site-plugin.git' )

// ...at its first table...
assert report( 'dependency-convergence' ).contains( 'org.slf4j:slf4j-api:jar:1.7.36:compile' )

// ...or at its first cell or image carrying attributes
assert report( 'dependencies' ).contains( 'slf4j-simple-1.7.36.jar' )
assert report( 'team' ).contains( 'https://www.gravatar.com/avatar/' )

// The other reports were never affected: check that they are still generated. mailing-lists, dependency-management
// and distribution-management are only rendered because the POM has the section they show. plugin-management is
// in the reportSet but not checked: this POM manages no plugins, so whether it is rendered is up to the Maven in use.
[ 'index', 'summary', 'mailing-lists', 'dependency-management', 'plugins', 'distribution-management' ].each {
    assert new File( basedir, "target/site/${it}.html" ).isFile() : "Report not generated: ${it}"
}

return true
