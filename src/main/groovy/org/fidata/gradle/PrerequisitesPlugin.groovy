#!/usr/bin/env groovy
/*
 * org.fidata.prerequisites Gradle plugin
 * Copyright © 2017-2018  Basil Peace
 *
 * This file is part of gradle-prerequisites-plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle

import static org.fidata.gradle.prerequisites.PrerequisiteTaskType.INSTALL
import static org.fidata.gradle.prerequisites.PrerequisiteTaskType.UPDATE
import static org.fidata.gradle.prerequisites.PrerequisiteTaskType.OUTDATED
import static org.fidata.gradle.prerequisites.PrerequisiteType.BUILD_TOOL
import org.fidata.gradle.prerequisites.PrerequisiteTaskType
import org.fidata.gradle.prerequisites.PrerequisiteType
import org.fidata.gradle.tasks.ResolveAndLockTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import org.gradle.api.artifacts.Configuration
import com.google.common.collect.TreeBasedTable
import com.google.common.collect.Table
import org.gradle.util.GradleVersion
import com.google.common.collect.Ordering

/**
 * Provides an environment for a general, language-agnostic project
 */
@CompileStatic
final class PrerequisitesPlugin implements Plugin<Project> {
  private Project project

  void apply(Project project) {
    this.project = project

    setupPrerequisitesLifecycleTasks()
    setupIntegrationForInstallUpdateTasks()
    setupIntegrationForOutdatedTasks()
  }

  private final Table<PrerequisiteTaskType, Optional<PrerequisiteType>, Task> tasks = TreeBasedTable.create(Ordering.<PrerequisiteTaskType>natural(), PrerequisiteType.comparator)

  /**
   * Gets name of common task
   * @param taskType type of task
   * @param type type of prerequisite
   * @return name of task
   */
  @SuppressWarnings(['MethodName'])
  static final String TASK_NAME(PrerequisiteTaskType taskType, Optional<PrerequisiteType> type) {
    "$taskType${ PrerequisiteType.getPluralName(type).capitalize() }"
  }

  private void setupPrerequisitesLifecycleTasks() {
    PrerequisiteTaskType.values().each { PrerequisiteTaskType taskType ->
      (PrerequisiteType.values() + [null]).collect { PrerequisiteType type -> Optional.ofNullable(type) }.each { Optional<PrerequisiteType> type ->
        String taskName = TASK_NAME(taskType, type)
        Task task = project.tasks.create(taskName) { Task task ->
          task.with {
            group = TaskConfiguration.GROUP
            description = taskType.description.call(PrerequisiteType.getPluralName(type))
          }
        }
        tasks.put(taskType, type, task)
        project.logger.debug('org.fidata.prerequisites: {} task created', task)
      }
      tasks.get(taskType, Optional.empty()).dependsOn tasks.row(taskType).findAll { Optional<PrerequisiteType> type, Task task -> type.present }.values()
      PrerequisiteType.values().each { PrerequisiteType type1 ->
        tasks.get(taskType, Optional.of(type1)).shouldRunAfter(tasks.row(taskType).findAll { Optional<PrerequisiteType> type2, Task task ->
          PrerequisiteType.comparator.compare(type2, Optional.of(type1)) > 0
        }.values())
      }
    }

    project.afterEvaluate {
      ((Collection<Task>)[
        tasks.row(INSTALL),
        tasks.row(OUTDATED)
      ]*.values().flatten()).each { Task runFirstTask ->
        // Recursively get all direct and indirect dependencies of task
        Set<Task> excludedTasks = []
        Set<Task> veryNewExcludedTasks = [runFirstTask].toSet()
        while (veryNewExcludedTasks.size() > 0) {
          Set<Task> newExcludedTasks = veryNewExcludedTasks
          excludedTasks += newExcludedTasks

          veryNewExcludedTasks = []
          newExcludedTasks.each { Task task ->
            veryNewExcludedTasks.addAll((Collection<Task>)task.taskDependencies.getDependencies(task))
            veryNewExcludedTasks.addAll((Collection<Task>)task.mustRunAfter.getDependencies(task))
            veryNewExcludedTasks.addAll((Collection<Task>)task.shouldRunAfter.getDependencies(task))
          }
          veryNewExcludedTasks -= excludedTasks
        }

        (project.tasks - excludedTasks).each { Task task ->
          task.mustRunAfter runFirstTask
        }
      }
    }
  }

  /**
   * Gets name of Gradle task
   * @param taskType type of task
   * @param type type of prerequisite
   * @return name of task
   */
  @SuppressWarnings(['MethodName'])
  static final String GRADLE_TASK_NAME(PrerequisiteTaskType taskType, PrerequisiteType type) {
    "${ taskType }Gradle${ PrerequisiteType.getPluralName(type).capitalize() }"
  }
  private void setupIntegrationForInstallUpdateTasks() {
    if (GradleVersion.current() >= GradleVersion.version('4.8')) {
      project.dependencyLocking.lockAllConfigurations()

      PrerequisiteType.values().each { PrerequisiteType type ->
        ResolveAndLockTask installTask = project.tasks.create(GRADLE_TASK_NAME(INSTALL, type), ResolveAndLockTask)
        installTask.configurationMatcher = { Configuration configuration ->
          PrerequisiteType.fromConfigurationName(configuration.name) == type &&
            /*
             * WORKAROUND:
             * org.gradle.internal.locking.LockFileReaderWriter.DEPENDENCY_LOCKING_FOLDER and FILE_SUFFIX
             * have package scope
             * <grv87 2018-07-08>
             */
            project.file("gradle/dependency-locks/${ configuration.name }.lockfile").exists()
        }
        tasks.get(INSTALL, Optional.of(type)).dependsOn installTask

        ResolveAndLockTask updateTask = project.tasks.create(GRADLE_TASK_NAME(UPDATE, type), ResolveAndLockTask)
        updateTask.configurationMatcher = { Configuration configuration ->
          PrerequisiteType.fromConfigurationName(configuration.name) == type
        }
        tasks.get(UPDATE, Optional.of(type)).dependsOn updateTask
      }
    }

    project.plugins.withId('nebula.dependency-lock') {
      Class<? extends Task> generateLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.GenerateLockTask')
      Class<? extends Task> updateLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.UpdateLockTask')
      Class<? extends Task> saveLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.SaveLockTask')

      project.tasks.withType(generateLockTaskClass) { Task task ->
        task.group = null
        tasks.get(INSTALL, Optional.empty()).mustRunAfter task
      }
      project.tasks.withType(updateLockTaskClass) { Task task ->
        task.group = null
        tasks.get(UPDATE, Optional.empty()).mustRunAfter task
      }
      project.tasks.withType(saveLockTaskClass) { Task task ->
        task.group = null
        tasks.get(UPDATE, Optional.empty()).mustRunAfter task
        switch (task.name) {
          case 'saveGlobalLock':
            task.dependsOn project.tasks.withType(updateLockTaskClass).getByName('updateGlobalLock')
            break
          case 'saveLock':
            task.dependsOn project.tasks.withType(updateLockTaskClass).getByName('updateLock')
            break
        }
      }
      tasks.get(UPDATE, Optional.empty()).with {
        if (((File)project.tasks.withType(saveLockTaskClass).findByName('saveGlobalLock')?.property('outputLock'))?.exists()) {
          dependsOn project.tasks.withType(saveLockTaskClass).getByName('saveGlobalLock')
        } else {
          dependsOn project.tasks.withType(saveLockTaskClass).getByName('saveLock')
        }
      }
    }

    project.plugins.withId('org.ajoberstar.stutter') {
      project.plugins.withId('java') {
        Task stutterWriteLocksTask = project.tasks.getByName('stutterWriteLocks')
        tasks.get(UPDATE, Optional.of(BUILD_TOOL)).dependsOn stutterWriteLocksTask
        stutterWriteLocksTask.group = null

        Task stutterWriteLocksIfNotExistTask = project.tasks.create('stutterWriteLocksIfNotExist') { Task task ->
          task.with {
            description = 'Generate lock files of Gradle versions to test for compatibility if they not already exist.'
            actions.addAll stutterWriteLocksTask.actions
            onlyIf {
              project.fileTree(dir: ((DirectoryProperty) project.extensions.getByName('stutter').properties['lockDir']).get(), includes: ['*.*']).empty
            }
          }
        }
        tasks.get(INSTALL, Optional.of(BUILD_TOOL)).dependsOn stutterWriteLocksIfNotExistTask
      }
    }
  }

  private void setupIntegrationForOutdatedTasks() {
    project.plugins.withId('com.github.ben-manes.versions') {
      project.tasks.withType((Class<? extends Task>)this.class.classLoader.loadClass('com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask')) { Task task ->
        task.group = null
        tasks.get(OUTDATED, Optional.empty()).dependsOn task
      }
    }

    project.plugins.withId('com.ofg.uptodate') {
      Task uptodateTask = project.tasks.getByName('uptodate')
      tasks.get(OUTDATED, Optional.empty()).dependsOn uptodateTask
      uptodateTask.group = null
    }
  }
}
