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
File dependencies = new File( basedir, 'target/site/dependencies.html' )
def mavenModel = '''\
<tr class="a">
<td align="left">maven-model-3.3.9.jar</td>
<td align="right">164 kB</td>
<td align="right">71</td>
<td align="right">54</td>
<td align="right">3</td>
<td align="center">1.7</td>
<td align="center">Yes</td></tr>
'''

def jacksonDataTypeJsr310 = '''\
<tr class="a">
<td align="left">jackson-datatype-jsr310-2.6.4.jar</td>
<td align="right">78.1 kB</td>
<td align="right">69</td>
<td align="right">51</td>
<td align="right">5</td>
<td align="center">1.8</td>
<td align="center">Yes</td></tr>
'''

assert dependencies.text.contains( mavenModel.replaceAll( "\n", System.getProperty( "line.separator" ) ) )
assert dependencies.text.contains( jacksonDataTypeJsr310.replaceAll( "\n", System.getProperty( "line.separator" ) ) )
