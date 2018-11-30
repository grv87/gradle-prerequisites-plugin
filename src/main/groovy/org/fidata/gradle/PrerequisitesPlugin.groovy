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
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static org.fidata.gradle.prerequisites.PrerequisiteType.PREREQUISITY
import static org.fidata.gradle.prerequisites.PrerequisiteType.BUILD_TOOL
import org.fidata.gradle.prerequisites.PrerequisiteTaskType
import org.fidata.gradle.prerequisites.PrerequisiteType
import org.fidata.gradle.tasks.ResolveAndLockTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.artifacts.Configuration
import com.google.common.collect.TreeBasedTable
import com.google.common.collect.Table
import org.gradle.util.GradleVersion

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

  private final Table<PrerequisiteTaskType, PrerequisiteType, Task> tasks = TreeBasedTable.create()

  /**
   * Gets name of common task
   * @param taskType type of task
   * @param type type of prerequisite
   * @return name of task
   */
  @SuppressWarnings('MethodName')
  static final String TASK_NAME(PrerequisiteTaskType taskType, PrerequisiteType type) {
    "$taskType${ type.pluralName.capitalize() }"
  }

  private void setupPrerequisitesLifecycleTasks() {
    PrerequisiteTaskType.values().each { PrerequisiteTaskType taskType ->
      PrerequisiteType.values().each { PrerequisiteType type ->
        String taskName = TASK_NAME(taskType, type)
        Task task = project.tasks.create(taskName) { Task task ->
          task.with {
            group = 'Build Setup' /* TaskConfiguration.GROUP was removed in Gradle 5.0 */
            description = taskType.description.call(type.pluralName)
          }
        }
        tasks.put(taskType, type, task)
        project.logger.debug('org.fidata.prerequisites: {} task created', task)
      }
      tasks.get(taskType, PREREQUISITY).dependsOn tasks.row(taskType).findAll { PrerequisiteType type, Task task -> type != PREREQUISITY }.values()
      PrerequisiteType.nonGenericValues().each { PrerequisiteType type1 ->
        tasks.get(taskType, type1).shouldRunAfter(tasks.row(taskType).findAll { PrerequisiteType type2, Task task -> type2 > type1 }.values())
      }
    }

    project.afterEvaluate {
      ((Collection<Task>)[
        tasks.row(INSTALL),
        tasks.row(UPDATE)
      ]*.values().flatten()).each { Task runFirstTask ->
        // Recursively get all direct and indirect dependencies of the task
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
  @SuppressWarnings('MethodName')
  static final String GRADLE_TASK_NAME(PrerequisiteTaskType taskType, PrerequisiteType type) {
    "${ taskType }Gradle${ type.pluralName.capitalize() }"
  }

  public static final String STUTTER_WRITE_LOCKS_IF_NOT_EXIST_TASK_NAME = 'stutterWriteLocksIfNotExist'

  private void setupIntegrationForInstallUpdateTasks() {
    if (GradleVersion.current() >= GradleVersion.version('4.8')) {
      project.dependencyLocking.lockAllConfigurations()

      PrerequisiteType.nonGenericValues().each { PrerequisiteType type ->
        ResolveAndLockTask installTask = project.tasks.create(GRADLE_TASK_NAME(INSTALL, type), ResolveAndLockTask)
        installTask.configurationMatcher = { Configuration configuration ->
          PrerequisiteType.fromConfigurationName(configuration.name) == type &&
            /*
             * WORKAROUND:
             * org.gradle.internal.locking.LockFileReaderWriter.DEPENDENCY_LOCKING_FOLDER and FILE_SUFFIX
             * have package scope
             * <grv87 2018-07-08>
             */
            !project.file("gradle/dependency-locks/${ configuration.name }.lockfile").exists()
        }
        tasks.get(INSTALL, type).dependsOn installTask

        ResolveAndLockTask updateTask = project.tasks.create(GRADLE_TASK_NAME(UPDATE, type), ResolveAndLockTask)
        updateTask.configurationMatcher = { Configuration configuration ->
          PrerequisiteType.fromConfigurationName(configuration.name) == type
        }
        tasks.get(UPDATE, type).dependsOn updateTask
      }
    }

    project.plugins.withId('nebula.dependency-lock') {
      Class<? extends Task> abstractLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.AbstractLockTask')
      Class<? extends Task> generateLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.GenerateLockTask')
      Class<? extends Task> updateLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.UpdateLockTask')
      Class<? extends Task> saveLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.SaveLockTask')
      Class<? extends Task> commitLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.CommitLockTask')

      project.tasks.withType(abstractLockTaskClass) { Task task ->
        task.group = null
      }

      project.tasks.withType(generateLockTaskClass) { Task task ->
        tasks.get(INSTALL, PREREQUISITY).dependsOn task
      }
      project.tasks.withType(updateLockTaskClass) { Task task ->
        tasks.get(UPDATE, PREREQUISITY).dependsOn task
      }
      project.tasks.withType(saveLockTaskClass) { Task task ->
        tasks.get(UPDATE, PREREQUISITY).dependsOn task
        switch (task.name) {
          case 'saveGlobalLock':
            task.dependsOn project.tasks.withType(updateLockTaskClass).getByName('updateGlobalLock')
            break
          case 'saveLock':
            task.dependsOn project.tasks.withType(updateLockTaskClass).getByName('updateLock')
            break
          default:
            project.logger.error('org.fidata.prerequisites: unknown task {} of type SaveLockTask', task.name)
        }
      }
      tasks.get(UPDATE, PREREQUISITY).with {
        if (((File)project.tasks.withType(saveLockTaskClass).findByName('saveGlobalLock')?.property('outputLock'))?.exists()) {
          dependsOn project.tasks.withType(saveLockTaskClass).getByName('saveGlobalLock')
        } else {
          dependsOn project.tasks.withType(saveLockTaskClass).getByName('saveLock')
        }
      }
      project.tasks.withType(commitLockTaskClass) { Task task ->
        task.enabled = false
      }
    }

    project.plugins.withId('org.ajoberstar.stutter') {
      project.plugins.withId('java') {
        Task stutterWriteLocksTask = project.tasks.getByName('stutterWriteLocks')
        tasks.get(UPDATE, BUILD_TOOL).dependsOn stutterWriteLocksTask
        stutterWriteLocksTask.group = null

        Task stutterWriteLocksIfNotExistTask = project.tasks.create(STUTTER_WRITE_LOCKS_IF_NOT_EXIST_TASK_NAME) { Task task ->
          task.with {
            description = 'Generate lock files of Gradle versions to test for compatibility if they not already exist.'
            onlyIf {
              project.fileTree(dir: ((DirectoryProperty) project.extensions.getByName('stutter').properties['lockDir']).get(), includes: ['*.*']).empty
            }
            actions.addAll stutterWriteLocksTask.actions
          }
        }
        tasks.get(INSTALL, BUILD_TOOL).dependsOn stutterWriteLocksIfNotExistTask
      }
    }
  }

  private void setupIntegrationForOutdatedTasks() {
    project.plugins.withId('com.github.ben-manes.versions') {
      project.tasks.withType((Class<? extends Task>)this.class.classLoader.loadClass('com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask')) { Task task ->
        task.group = null
        tasks.get(OUTDATED, PREREQUISITY).dependsOn task
      }
    }

    project.plugins.withId('com.ofg.uptodate') {
      Task uptodateTask = project.tasks.getByName('uptodate')
      tasks.get(OUTDATED, PREREQUISITY).dependsOn uptodateTask
      uptodateTask.group = null
    }
  }
}
