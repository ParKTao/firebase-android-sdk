// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.gradle.plugins;

import com.google.common.collect.ImmutableSet;
import com.google.firebase.gradle.plugins.ci.device.FirebaseTestLabExtension;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;

public class FirebaseLibraryExtension {
  private final Project project;
  private final Set<FirebaseLibraryExtension> librariesToCoRelease = new HashSet<>();

  /** Indicates whether the library has public javadoc. */
  public boolean publishJavadoc = true;

  /** Indicates whether sources are published alongside the library. */
  public boolean publishSources;

  /** Firebase Test Lab configuration/ */
  public final FirebaseTestLabExtension testLab;

  @Inject
  public FirebaseLibraryExtension(Project project) {
    this.project = project;
    this.testLab = new FirebaseTestLabExtension(project.getObjects());
  }

  /** Configure Firebase Test Lab. */
  public void testLab(Action<FirebaseTestLabExtension> action) {
    action.execute(testLab);
  }

  /**
   * Register to be released alongside another Firebase Library project.
   *
   * <p>This will force the released version of the current project to match the one it's released
   * with.
   */
  public void releaseWith(Project releaseWithProject) {
    try {
      FirebaseLibraryExtension releaseWithLibrary =
          releaseWithProject.getExtensions().getByType(FirebaseLibraryExtension.class);
      releaseWithLibrary.librariesToCoRelease.add(this);
      this.project.setVersion(releaseWithProject.getVersion());

      String latestRelease = "latestReleasedVersion";
      if (releaseWithProject.getExtensions().getExtraProperties().has(latestRelease)) {
        this.project
            .getExtensions()
            .getExtraProperties()
            .set(latestRelease, releaseWithProject.getProperties().get(latestRelease));
      }

    } catch (UnknownDomainObjectException ex) {
      throw new GradleException(
          "Library cannot be released with a project that is not a Firebase Library itself");
    }
  }

  public Set<Project> getProjectsToRelease() {
    return ImmutableSet.<Project>builder()
        .add(project)
        .addAll(librariesToCoRelease.stream().map(l -> l.project).collect(Collectors.toSet()))
        .build();
  }
}
