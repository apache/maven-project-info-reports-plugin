---
title: Frequently Asked Questions
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

<a id="top"></a>

# Frequently Asked Questions

1. [Is it possible to customize the labels of the Project Reports Menu?](#Is_it_possible_to_customize_the_labels_of_the_Project_Reports_Menu)
2. [How to exclude SCM access info?](#How_to_exclude_SCM_anonymous_access_info)
3. [How to use developer/contributor time zone?](#How_to_use_developer.2Fcontributor_time_zone)
4. [Is it possible to customize the reports rendering?](#Is_it_possible_to_customize_the_reports_rendering)
5. [Why does the Licenses report link to my license instead of showing its text?](#Why_does_the_Licenses_report_link_to_my_license_instead_of_showing_its_text)

<a id="Is_it_possible_to_customize_the_labels_of_the_Project_Reports_Menu"></a>

### Is it possible to customize the labels of the Project Reports Menu?

Yes. The menu shows each report's name, which comes from the report's text bundle. Override the
`report.<report>.name` key in a [custom bundle](./examples/custom-report.html), for example
`report.dependencies.name = Third-Party Libraries`.

<a id="How_to_exclude_SCM_anonymous_access_info"></a>

### How to exclude SCM access info?

See the [SCM report documentation](examples/scm-report.html#How_to_exclude_SCM_anonymous_access_info).

<a id="How_to_use_developer.2Fcontributor_time_zone"></a>
<a id="How_to_use_developercontributor_time_zone"></a>

### How to use developer/contributor time zone?

Set `<timezone>` on each `<developer>` or `<contributor>` in the POM, for example
`<timezone>Europe/Berlin</timezone>`; the Team report adds a Time Zone column with the value. See the [developers section](https://maven.apache.org/pom.html#Developers) of the POM reference.

<a id="Is_it_possible_to_customize_the_reports_rendering"></a>

### Is it possible to customize the reports rendering?

No. The layout of each report is fixed in the plugin; you can only [customize](./examples/custom-report.html)
the texts. Proposals are welcome in the [issue tracker](https://github.com/apache/maven-project-info-reports-plugin/issues).

<a id="Why_does_the_Licenses_report_link_to_my_license_instead_of_showing_its_text"></a>

### Why does the Licenses report link to my license instead of showing its text?

Since version 3.10.0, a license given by an `http` or `https` URL is rendered as a link instead of being downloaded and inlined, so that building a site does not send a request to the server hosting each license. A license in a local file, for example `<url>LICENSE.txt</url>` next to the POM, is still inlined.

To download remote licenses and inline their text as before, set [`downloadRemoteLicenses`](./licenses-mojo.html#downloadRemoteLicenses) to `true`, or pass `-Dlicenses.downloadRemote=true`.
