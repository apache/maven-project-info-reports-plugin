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
File log = new File( basedir, 'build.log' )
String logContent = log.text

// the cache files of a different version are ignored, and the jar files are analyzed again
assert logContent.contains( 'JarDataSummary analyzed for: org.apache.commons:commons-math3:jar:tools:3.6.1:compile' )
assert logContent.contains( 'JarDataSummary analyzed for: org.apache.maven.its.mpir-465:snapshot-test:jar:1.0-SNAPSHOT:compile' )
assert logContent.contains( 'JarDataSummary analyzed for: org.codehaus.plexus:plexus-utils:jar:4.0.0:compile' )
assert !logContent.contains( 'JarDataSummary cached for:' )

// ...which is not a failure to read the cache file
assert !logContent.contains( 'Loading JarDataSummary from cache failed' )

// the configured directory is used, and its cache files are replaced by the ones of the current version
File customCacheDir = new File( basedir, 'target/custom-cache' )
[ 'org/codehaus/plexus/plexus-utils/4.0.0', 'org/apache/maven/its/mpir-465/snapshot-test/1.0-SNAPSHOT', 'org/apache/commons/commons-math3/3.6.1/tools' ].each { String artifactPath ->
    File cacheFile = new File( customCacheDir, artifactPath + '/jar-data.properties' )
    Properties props = new Properties()
    cacheFile.withReader( 'UTF-8' ) { Reader reader -> props.load( reader ) }
    assert props.getProperty( 'v' ) == '1' : "${cacheFile} was not replaced"
}

// ...and the default one is not
assert !new File( basedir, 'target/mpir-cache' ).exists()

return true
