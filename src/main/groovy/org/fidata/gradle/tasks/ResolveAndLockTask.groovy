#!/usr/bin/env groovy
/*
 * ResolveAndLockTask Gradle task class
 * Copyright ©  Basil Peace
 *
 * This file is part of gradle-prerequisites-plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

/**
 * Task to resolve and lock configurations
 */
@CompileStatic
class ResolveAndLockTask extends DefaultTask {
  @Internal
  Closure<Boolean> configurationMatcher = { Configuration configuration -> true }

  @TaskAction
  void resolveAndLock() {
    if (!project.gradle.startParameter.writeDependencyLocks) {
      throw new IllegalArgumentException(String.sprintf('%s: this task should be run with `--write-locks` argument', name))
    }

    project.configurations.matching { Configuration configuration ->
      configuration.canBeResolved &&
      /*
       * WORKAROUND:
       * CodeNarc doesn't work with its configuration locked
       * https://github.com/gradle/gradle/issues/5894
       * <grv87 2018-07-08>
       */
      (
        GradleVersion.current() >= GradleVersion.version('4.9-rc-2') ||
        configuration.name != 'codenarc'
      ) &&
      configurationMatcher.call(configuration)
    }.each { Configuration configuration ->
      configuration.resolve()
    }
  }
}
