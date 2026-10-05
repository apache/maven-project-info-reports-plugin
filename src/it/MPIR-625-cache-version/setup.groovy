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
def cacheDir = new File( basedir, 'target/custom-cache' )
cacheDir.deleteDir()

//
// Write cache files as if they were written by a future version of the plugin, with a different format. They must
// be ignored (and not be considered a failure): the jar files are analyzed again.
//
[ 'org/codehaus/plexus/plexus-utils/4.0.0', 'org/apache/maven/its/mpir-465/snapshot-test/1.0-SNAPSHOT', 'org/apache/commons/commons-math3/3.6.1/tools' ].each { String artifactPath ->
    File cacheFile = new File( cacheDir, artifactPath + '/jar-data.properties' )
    cacheFile.parentFile.mkdirs()
    cacheFile.text = "v=2\nsomeFieldOfTheFuture=1\n"
}

return true