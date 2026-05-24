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

def cacheDir = new File(localRepositoryPath, '.cache/mpir')
cacheDir.deleteDir();

//
// Modify cached summary data so that it forces to re analyze the actual jar file.
//

def artifactCacheFile = new File(cacheDir, 'org/codehaus/plexus/plexus-utils/4.0.0/jar-data.json')
artifactCacheFile.parentFile.mkdirs()
artifactCacheFile.text = '%& corrupted json file ((())'


def artifactFile2 = new File(localRepositoryPath, 'org/apache/maven/its/mpir-465/snapshot-test/1.0-SNAPSHOT/snapshot-test-1.0-SNAPSHOT.jar')
assert artifactFile2.exists(), "Artifact file ${artifactFile2} does not exist"
// set the last modified time of the cache to the past, so that the cache is considered stale and will be updated by the plugin
long artifactFile2LastModified = artifactFile2.lastModified() - 1L

def artifactCacheFile2 = new File(cacheDir, 'org/apache/maven/its/mpir-465/snapshot-test/1.0-SNAPSHOT/jar-data.json')
artifactCacheFile2.parentFile.mkdirs()
artifactCacheFile2.text = """\
{
  "v": 1,
  "numEntries": 37,
  "numClasses": 1,
  "numPackages": 1,
  "jdkRevision": "1.8",
  "debugPresent": true,
  "multiRelease": true,
  "versionedRuntimes": [
    {
      "debugPresent": true,
      "numEntries": 10,
      "numClasses": 1,
      "numPackages": 1,
      "jdkRevision": "9"
    },
    {
      "debugPresent": true,
      "numEntries": 10,
      "numClasses": 1,
      "numPackages": 1,
      "jdkRevision": "11"
    }
  ],
  "numRootEntries": 17,
  "fsize": 10201,
  "ts": ${artifactFile2LastModified},
  "sealed": false
}"""

def artifactFile3 = new File(localRepositoryPath, 'org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1-tools.jar')
assert artifactFile3.exists(), "Artifact file ${artifactFile3} does not exist"
// set the file size of the cache different to the real file size, so that the cache is considered stale and will be updated by the plugin
long artifactFile3FileSize = artifactFile3.length() + 1L
long artifactFile3LastModified = artifactFile3.lastModified()

def artifactCacheFile3 = new File(cacheDir, 'org/apache/commons/commons-math3/3.6.1/tools/jar-data.json')
artifactCacheFile3.parentFile.mkdirs()
artifactCacheFile3.text = """\
{
  "v": 1,
  "numEntries": 10,
  "numClasses": 2,
  "numPackages": 1,
  "jdkRevision": "1.5",
  "debugPresent": true,
  "multiRelease": false,
  "versionedRuntimes": null,
  "numRootEntries": 0,
  "fsize": ${artifactFile3FileSize},
  "ts": ${artifactFile3LastModified},
  "sealed": false
}"""

return true