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

String[] reports = ["dependency-info", "index", "plugin-management", "plugins", "project-info", "summary"] as String[]

String[] skippedReports = [
    "dependencies",
    "integration",
    "issue-management",
    "licenses",
    "mailing-lists",
    "scm",
    "dependency-management",
    "team",
    "distribution-management",
    "modules"
] as String[]

for ( String report : reports ) {
    File reportFile = new File( siteDir, report + ".html" )
    assert reportFile.isFile() : "Report file not existent: " + reportFile
}

for ( String skippedReport : skippedReports ) {
    File skippedReportFile = new File( siteDir, skippedReport + ".html" )
    assert !skippedReportFile.isFile() : "Skipped report file existent: " + skippedReportFile
}
