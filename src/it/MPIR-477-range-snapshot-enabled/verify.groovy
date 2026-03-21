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
String html = new File(basedir, "target/reports/dependency-management.html").text.normalize()
// assert the resolved version by using the <url> of the dependency in its pom.xml file, since the version is not printed in the reportR
assert html.contains("https://mpir477.example.org/snapshot-1.2") :
    "range [1.0,) against a repository with snapshots enabled should resolve to 1.2-SNAPSHOT"
assert !html.contains("release-1.1")
