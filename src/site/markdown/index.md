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
The Maven Project Info Reports plugin is used to generate reports information about the project.

## Goals Overview

The Project Info Reports Plugin has the following goals:

- [project-info-reports:ci-management](./ci-management-mojo.html) is used to generate the Project Continuous Integration Management report.
- [project-info-reports:dependencies](./dependencies-mojo.html) is used to generate the Project Dependencies report.
- [project-info-reports:dependency-convergence](./dependency-convergence-mojo.html) is used to generate the Project Dependency Convergence report for (reactor) builds.
- [project-info-reports:dependency-info](./dependency-info-mojo.html) is used to generate code snippets of the Maven coordinates to be added to build tools.
- [project-info-reports:dependency-management](./dependency-management-mojo.html) is used to generate the Project Dependency Management report.
- [project-info-reports:distribution-management](./distribution-management-mojo.html) is used to generate the Project Distribution Management report.
- [project-info-reports:help](./help-mojo.html) is used to display help information on the Project Info Reports Plugin.
- [project-info-reports:index](./index-mojo.html) is used to generate the Project index page.
- [project-info-reports:issue-management](./issue-management-mojo.html) is used to generate the Project Issue Management report.
- [project-info-reports:licenses](./licenses-mojo.html) is used to generate the Project Licenses report.
- [project-info-reports:mailing-lists](./mailing-lists-mojo.html) is used to generate the Project Mailing Lists report.
- [project-info-reports:modules](./modules-mojo.html) is used to generate the Project Modules report.
- [project-info-reports:plugin-management](./plugin-management-mojo.html) is used to generate the Project Plugin Management report.
- [project-info-reports:plugins](./plugins-mojo.html) is used to generate the Project Plugins report.
- [project-info-reports:team](./team-mojo.html) is used to generate the Project Team report.
- [project-info-reports:scm](./scm-mojo.html) is used to generate the Project Source Code Management report.
- [project-info-reports:summary](./summary-mojo.html) is used to generate the Project Summary report.
## Upcoming Incompatibility Notice

With a future version mojos and output filenames will change for consistency alignment. See the following table for the upcoming changes:

|Goal Name|New Goal Name|Output name|New Output Name|
|:---:|:---:|:---:|:---:|
|`dependency-info`|`maven-coordinates`|`dependency-info.html`|`maven-coordinates.html`|

## Usage

General instructions on how to use the Project Info Reports Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](http://maven.apache.org/guides/development/guide-helping.html).

## Examples

To provide you with better understanding on some usages of the Maven Project Info Reports Plugin, you can take a look into the following examples:

- [Run Selective Reports](./examples/selective-reports.html)
- [Run Individual Reports](./examples/individual-reports.html)
- [Customize the SCM Report](./examples/scm-report.html)
- [Overview of the reports generated for this plugin](./project-info.html)
## Related Links

- [Localization of Plugins](http://maven.apache.org/plugins/localization.html)
- [Create a layout (skin))](http://maven.apache.org/plugins/maven-site-plugin/examples/creatingskins.html)
