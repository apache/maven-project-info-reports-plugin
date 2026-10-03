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
package org.apache.maven.report.projectinfo.dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * A node of the dependency tree shown by the reports: an artifact and the nodes it brings in.
 *
 * @since 3.9.1
 */
public class DependencyNode {
    private final DependencyNode parent;

    private final Artifact artifact;

    private final boolean optional;

    private List<DependencyNode> children = Collections.emptyList();

    DependencyNode(DependencyNode parent, Artifact artifact, boolean optional) {
        this.parent = parent;
        this.artifact = artifact;
        this.optional = optional;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public DependencyNode getParent() {
        return parent;
    }

    public List<DependencyNode> getChildren() {
        return children;
    }

    void setChildren(List<DependencyNode> children) {
        this.children = Collections.unmodifiableList(children);
    }

    public String toNodeString() {
        return artifact + (optional ? " (optional)" : "");
    }

    /**
     * Visits this node and its descendants depth-first.
     *
     * @param visitor the visitor
     * @return the result of {@link DependencyNodeVisitor#endVisit(DependencyNode)} for this node
     */
    public boolean accept(DependencyNodeVisitor visitor) {
        if (visitor.visit(this)) {
            for (DependencyNode child : children) {
                if (!child.accept(visitor)) {
                    break;
                }
            }
        }
        return visitor.endVisit(this);
    }

    /**
     * Copies this tree keeping only the nodes that match the filter and their ancestors.
     *
     * @param filter the filter
     * @return the pruned copy, or <code>null</code> if no node matches
     */
    public DependencyNode prune(ArtifactFilter filter) {
        return prune(null, filter);
    }

    private DependencyNode prune(DependencyNode newParent, ArtifactFilter filter) {
        DependencyNode copy = new DependencyNode(newParent, artifact, optional);
        List<DependencyNode> copiedChildren = new ArrayList<>();
        for (DependencyNode child : children) {
            DependencyNode copiedChild = child.prune(copy, filter);
            if (copiedChild != null) {
                copiedChildren.add(copiedChild);
            }
        }
        if (copiedChildren.isEmpty() && !filter.include(artifact)) {
            return null;
        }
        copy.setChildren(copiedChildren);
        return copy;
    }
}
