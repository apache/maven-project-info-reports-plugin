<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Maven Project Info Reports Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-project-info-reports-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-project-info-reports-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/plugins/maven-project-info-reports-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-project-info-reports-plugin/README.md)
[![Verify](https://github.com/apache/maven-project-info-reports-plugin/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-project-info-reports-plugin/actions/workflows/maven-verify.yml)

Generates the "Project Information" pages of a Maven site from what the POM declares:
dependencies, Maven coordinates, team, licenses, mailing lists, source repository, issue
tracker, CI, distribution management, plugins and modules. The
[Maven Site Plugin](https://maven.apache.org/plugins/maven-site-plugin/) runs these reports
during `mvn site`; each one can also run on its own, for example
`mvn project-info-reports:dependencies`.

The plugin requires Maven 3.9.0 or later and Java 8 or later.

- Documentation: <https://maven.apache.org/plugins/maven-project-info-reports-plugin/>
- Issues: <https://github.com/apache/maven-project-info-reports-plugin/issues>
- Release notes: <https://github.com/apache/maven-project-info-reports-plugin/releases>

## Build

```shell
mvn verify
```

Run the integration tests in `src/it` with the `run-its` profile:

```shell
mvn -Prun-its verify
```

Build the plugin site, including the goal pages, with:

```shell
mvn -Preporting site
```

## Contributing

Open an issue or a pull request against `master`. Code style is enforced by Spotless:
run `mvn spotless:apply` before pushing. The
[Maven development guide](https://maven.apache.org/guides/development/guide-helping.html)
covers the rest.
