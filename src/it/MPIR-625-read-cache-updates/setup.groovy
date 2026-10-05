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
def cacheDir = new File( basedir, 'target/mpir-cache' )
cacheDir.deleteDir()

def writeCache = { String artifactPath, String content ->
    File cacheFile = new File( cacheDir, artifactPath + '/jar-data.properties' )
    cacheFile.parentFile.mkdirs()
    cacheFile.text = content
}

// the entries of the multi-release runtimes, in the format of the cache file
def runtimes = { int numEntries, List<String> jdkRevisions ->
    StringBuilder sb = new StringBuilder( "versionedRuntimes=${jdkRevisions.size()}\n" )
    jdkRevisions.eachWithIndex { String jdkRevision, int i ->
        sb << "versionedRuntimes.${i}.debugPresent=true\n"
        sb << "versionedRuntimes.${i}.numEntries=${numEntries}\n"
        sb << "versionedRuntimes.${i}.numClasses=1\n"
        sb << "versionedRuntimes.${i}.numPackages=1\n"
        sb << "versionedRuntimes.${i}.jdkRevision=${jdkRevision}\n"
    }
    return sb.toString()
}

def localRepoFile = { String path ->
    File file = new File( localRepositoryPath, path )
    assert file.exists() : "Artifact file ${file} does not exist"
    return file
}

//
// Write cached summaries which are unusable, so that the actual jar files are analyzed again.
//

// not a valid cache file
writeCache( 'org/codehaus/plexus/plexus-utils/4.0.0', '%& corrupted cache file ((())' )

// the last modified time in the cache is different to that of the file, so that the cache is considered stale
File snapshotJar = localRepoFile( 'org/apache/maven/its/mpir-465/snapshot-test/1.0-SNAPSHOT/snapshot-test-1.0-SNAPSHOT.jar' )
writeCache( 'org/apache/maven/its/mpir-465/snapshot-test/1.0-SNAPSHOT', """\
v=1
sealed=false
numEntries=37
numClasses=1
numPackages=1
jdkRevision=1.8
debugPresent=true
multiRelease=true
numRootEntries=17
fsize=${snapshotJar.length()}
ts=${snapshotJar.lastModified() - 1L}
""" + runtimes( 10, ['9', '11'] ) )

// the file size in the cache is different to that of the file, so that the cache is considered stale
File mathJar = localRepoFile( 'org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1-tools.jar' )
writeCache( 'org/apache/commons/commons-math3/3.6.1/tools', """\
v=1
sealed=false
numEntries=10
numClasses=2
numPackages=1
jdkRevision=1.5
debugPresent=true
multiRelease=false
numRootEntries=0
fsize=${mathJar.length() + 1L}
ts=${mathJar.lastModified()}
""" )

return true