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
assert !( log.text =~ /\[WARNING\] Unable to create Maven project for com\.sun:javaws:pom:.* from repository\./ )

// the only managed dependency here declares an explicit version, so nothing was resolved from the
// dependency tree and the "resolved version" note must not be shown
File report = new File( basedir, 'target/site/dependency-management.html' )
assert report.exists() : 'The dependency-management report was not generated'
assert !report.text.contains( 'Versions shown in parentheses' ) : 'The resolved-version note should not be shown when no version was resolved'
