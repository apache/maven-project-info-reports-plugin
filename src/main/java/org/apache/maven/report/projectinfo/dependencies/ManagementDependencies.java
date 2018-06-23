package org.apache.maven.report.projectinfo.dependencies;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 * @author Nick Stolwijk
 * @since 2.1
 */
public class ManagementDependencies
{
    private final List<Dependency> dependencies;

    /**
     * @param projectDependencies the list of dependencies.
     */
    public ManagementDependencies( List<Dependency> projectDependencies )
    {
        this.dependencies = projectDependencies;
    }

    /**
     * @return <code>true</code> if dependencies is not null and not empty.
     */
    public boolean hasDependencies()
    {
        return ( dependencies != null ) && ( !this.dependencies.isEmpty() );
    }

    /**
     * @return dependencies
     */
    public List<Dependency> getManagementDependencies()
    {
        return new ArrayList<>( dependencies );
    }

    /**
     * @return the dependencies by scope
     * @see Artifact#SCOPE_COMPILE
     * @see Artifact#SCOPE_PROVIDED
     * @see Artifact#SCOPE_RUNTIME
     * @see Artifact#SCOPE_SYSTEM
     * @see Artifact#SCOPE_TEST
     */
    public Map<String, List<Dependency>> getManagementDependenciesByScope()
    {
        Map<String, List<Dependency>> dependenciesByScope = new HashMap<>();
        for ( Dependency dependency : dependencies )
        {
            String scope = dependency.getScope() != null ? dependency.getScope() : Artifact.SCOPE_COMPILE;
            List<Dependency> multiValue = dependenciesByScope.get( scope );
            if ( multiValue == null )
            {
                multiValue = new ArrayList<>();
            }
            multiValue.add( dependency );
            dependenciesByScope.put( scope, multiValue );
        }

        return dependenciesByScope;
    }
}
