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

// without cache the jar files are always analyzed
assert logContent.contains( 'JarDataSummary analyzed for: org.apache.commons:commons-math3:jar:tools:3.6.1:compile' )
assert logContent.contains( 'JarDataSummary analyzed for: org.apache.maven.its.mpir-465:snapshot-test:jar:1.0-SNAPSHOT:compile' )
assert logContent.contains( 'JarDataSummary analyzed for: org.codehaus.plexus:plexus-utils:jar:4.0.0:compile' )
assert !logContent.contains( 'JarDataSummary cached for:' )

// ...and nothing is written to disk
new File( basedir, 'target' ).eachFileRecurse { File file ->
    assert file.name != 'jar-data.properties' : "The cache file ${file} was written"
}
assert !new File( basedir, 'target/mpir-cache' ).exists()

return true
