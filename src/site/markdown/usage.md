---
title: Usage
author: 
  - Johnny R. Ruiz III, Pete Marvin King
date: 2009-07-14
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Usage

Maven has been configured to create the project info reports by default. There's no need to configure anything in your `pom.xml` to generate the project information reports.

Simply running `mvn site` would generate the project information documentation. For more information on how to customize the Project Information Reports Plugin, check the examples on the [Introduction](./index.html) page.

## Required Maven Site Plugin version

A report plugin does not render with its own Doxia. It renders with the Doxia that the
[Maven Site Plugin](https://maven.apache.org/plugins/maven-site-plugin/history.html#maven-site-plugin-vs-doxia-vs-doxia-sitetools)
in use provides, so the version of the Site Plugin decides which Doxia the reports get.

Versions **3.7.0 to 3.9.0** of this plugin are built against Doxia 2 and need
**Maven Site Plugin 3.21.0 or newer**, which is the first version providing it.

This is easy to run into without changing anything, because Maven 3.9.x still binds Maven Site Plugin
**3.12.1** by default, and that version provides Doxia 1. The symptom is a warning per affected report
and a report that stops part way through, while the build still reports success:

```
[WARNING] An issue has occurred with maven-project-info-reports-plugin:3.9.0:issue-management report,
skipping LinkageError 'void org.apache.maven.doxia.sink.Sink.verbatim()',
please report an issue to Maven dev team.
```

The reports affected are `ci-management`, `dependency-info`, `issue-management`, `licenses` and `scm`.

To avoid it, declare the Site Plugin version explicitly rather than relying on the default:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-site-plugin</artifactId>
      <version>3.22.0</version>
    </plugin>
  </plugins>
</build>
```
