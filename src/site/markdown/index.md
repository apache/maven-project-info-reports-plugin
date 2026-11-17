---
title: Introduction
author: 
  - Johnny R. Ruiz III
  - jruiz@exist.com
date: 2013-07-22
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

# Apache Maven Project Info Reports Plugin

The Project Info Reports Plugin turns what your POM declares into the "Project Information"
pages of a Maven site: dependencies, Maven coordinates, team, licenses, mailing lists, source
repository, issue tracker, continuous integration, distribution management, plugins and modules.

The plugin requires Maven 3.9.0 or later and Java 8 or later.

Most users come for one of these tasks:

- Generate all project information pages with the rest of the site: run `mvn site` and open
  `target/site/project-info.html`. See the [usage page](./usage.html).
- Generate one report without building the site, for example the dependency list:
  `mvn project-info-reports:dependencies` writes `target/reports/dependencies.html`.
  See [Run Individual Reports](./examples/individual-reports.html).
- Choose which reports the site contains: see [Run Selective Reports](./examples/selective-reports.html).
- Change the texts or menu labels of the reports: see [Customize Text Reports](./examples/custom-report.html).

## Goals Overview

The plugin has the following goals. Each report goal can run inside `mvn site` or on its own.

- [project-info-reports:ci-management](./ci-management-mojo.html) generates the CI Management report.
- [project-info-reports:dependencies](./dependencies-mojo.html) generates the Dependencies report.
- [project-info-reports:dependency-convergence](./dependency-convergence-mojo.html) generates the Dependency Convergence report for a single project or a reactor build.
- [project-info-reports:dependency-info](./dependency-info-mojo.html) generates the Maven Coordinates report: snippets for declaring the project as a dependency in Maven, Ivy, Grape, Gradle, SBT and Leiningen; for a project with `maven-plugin` packaging, the plugin declaration.
- [project-info-reports:dependency-management](./dependency-management-mojo.html) generates the Dependency Management report.
- [project-info-reports:distribution-management](./distribution-management-mojo.html) generates the Distribution Management report.
- [project-info-reports:help](./help-mojo.html) displays help information on the plugin.
- [project-info-reports:index](./index-mojo.html) generates the About page of the project.
- [project-info-reports:issue-management](./issue-management-mojo.html) generates the Issue Management report.
- [project-info-reports:licenses](./licenses-mojo.html) generates the Licenses report.
- [project-info-reports:mailing-lists](./mailing-lists-mojo.html) generates the Mailing Lists report.
- [project-info-reports:modules](./modules-mojo.html) generates the Project Modules report.
- [project-info-reports:plugin-management](./plugin-management-mojo.html) generates the Plugin Management report.
- [project-info-reports:plugins](./plugins-mojo.html) generates the Plugins report.
- [project-info-reports:scm](./scm-mojo.html) generates the Source Code Management report.
- [project-info-reports:summary](./summary-mojo.html) generates the Summary report.
- [project-info-reports:team](./team-mojo.html) generates the Team report.

<a id="Upcoming_Incompatibility_Notice"></a>
Since version 3.6.0 the `dependency-info` report is titled Maven Coordinates; the goal and its
output file `dependency-info.html` keep their names.

## Usage

General instructions are on the [usage page](./usage.html), and the [FAQ](./faq.html) answers
common questions. This plugin's own [project information pages](./project-info.html) show what
each report looks like.

Ask questions on the [user mailing list](./mailing-lists.html), and report a defect or request a
feature in the [issue tracker](./issue-management.html). For a defect, a small project that
reproduces it helps most. To contribute a fix, see the [source repository](./scm.html) and the
[guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

## Examples

The following examples cover common configurations:

- [Run Selective Reports](./examples/selective-reports.html)
- [Run Individual Reports](./examples/individual-reports.html)
- [Customize the SCM Report](./examples/scm-report.html)
- [Customize Text Reports](./examples/custom-report.html)

## Related Links

- [Localization of plugins](https://maven.apache.org/plugins/localization.html)
- [Creating a site skin](https://maven.apache.org/plugins/maven-site-plugin/examples/creatingskins.html)
