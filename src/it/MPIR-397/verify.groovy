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
File report = new File( basedir, 'target/reports/dependency-management.html' );
assert report.exists() : 'The dependency-management report was not generated';

String html = report.text.normalize();

// the versionless managed dependency must appear in the report
assert html.contains( 'multi-release-test' ) : 'The managed dependency is missing from the report';

// its version must be resolved from the dependency tree
assert html.contains( '>(0.0.1)<' ) : 'The resolved version 0.0.1 is missing from the report';

// a note explaining the parentheses notation must be shown, since a version was actually resolved
assert html.contains( 'Versions shown in parentheses' ) : 'The resolved-version note is missing from the report';

File log = new File( basedir, 'build.log' );
assert !log.text.contains( 'The version cannot be empty' ) : 'The version resolution failed';

return true;