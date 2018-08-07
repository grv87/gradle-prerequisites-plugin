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

import com.google.common.collect.Table
import com.google.common.collect.TreeBasedTable
import groovy.transform.CompileStatic
import org.fidata.gradle.prerequisites.PrerequisiteTaskType
import org.fidata.gradle.prerequisites.PrerequisiteType
import org.fidata.gradle.tasks.ResolveAndLockTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import org.gradle.util.GradleVersion

import static org.fidata.gradle.prerequisites.PrerequisiteTaskType.INSTALL
import static org.fidata.gradle.prerequisites.PrerequisiteTaskType.OUTDATED
import static org.fidata.gradle.prerequisites.PrerequisiteTaskType.UPDATE
import static org.fidata.gradle.prerequisites.PrerequisiteType.BUILD_TOOL
import static org.fidata.gradle.prerequisites.PrerequisiteType.PREREQUISITY

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

  private final Table<PrerequisiteTaskType, PrerequisiteType, TaskProvider<Task>> taskProviders = TreeBasedTable.create()

  /**
   * Gets name of common task
   * @param taskType type of task
   * @param type type of prerequisite
   * @return name of task
   */
  @SuppressWarnings(['MethodName'])
  static final String TASK_NAME(PrerequisiteTaskType taskType, PrerequisiteType type) {
    "$taskType${ type.pluralName.capitalize() }"
  }

  private void setupPrerequisitesLifecycleTasks() {
    PrerequisiteTaskType.values().each { PrerequisiteTaskType taskType ->
      PrerequisiteType.values().each { PrerequisiteType type ->
        String taskName = TASK_NAME(taskType, type)
        TaskProvider taskProvider = project.tasks.register(taskName) { Task task ->
          task.with {
            group = TaskConfiguration.GROUP
            description = taskType.description.call(type.pluralName)
          }
        }
        taskProviders.put(taskType, type, taskProvider)
        project.logger.debug('org.fidata.prerequisites: {} task created', taskProvider)
      }
      taskProviders.get(taskType, PREREQUISITY).configure { Task task -> task.dependsOn taskProviders.row(taskType).findAll { PrerequisiteType type, TaskProvider<Task> taskProvider -> type != PREREQUISITY }.values() }
      PrerequisiteType.nonGenericValues().each { PrerequisiteType type1 ->
        taskProviders.get(taskType, type1).configure { Task task -> task.shouldRunAfter(taskProviders.row(taskType).findAll { PrerequisiteType type2, TaskProvider<Task> taskProvider -> type2 > type1 }.values()) }
      }
    }

    project.afterEvaluate {
      ((Collection<TaskProvider<Task>>)[
        taskProviders.row(INSTALL),
        taskProviders.row(UPDATE)
      ]*.values().flatten()).each { TaskProvider<Task> runFirstTask ->
        // Recursively get all direct and indirect dependencies of the task
        Set<Task> excludedTasks = []
        Set<Task> veryNewExcludedTasks = [runFirstTask.get()].toSet()
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

        project.tasks.matching { Task task -> !excludedTasks.contains(task) }.configureEach { Task task ->
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
    "${ taskType }Gradle${ type.pluralName.capitalize() }"
  }

  public static final String STUTTER_WRITE_LOCKS_IF_NOT_EXIST_TASK_NAME = 'stutterWriteLocksIfNotExist'

  private void setupIntegrationForInstallUpdateTasks() {
    if (GradleVersion.current() >= GradleVersion.version('4.8')) {
      project.dependencyLocking.lockAllConfigurations()

      PrerequisiteType.nonGenericValues().each { PrerequisiteType type ->
        TaskProvider<ResolveAndLockTask> installTaskProvider = project.tasks.register(GRADLE_TASK_NAME(INSTALL, type), ResolveAndLockTask) { ResolveAndLockTask resolveAndLockTask ->
          resolveAndLockTask.configurationMatcher = { Configuration configuration ->
            PrerequisiteType.fromConfigurationName(configuration.name) == type &&
              /*
             * WORKAROUND:
             * org.gradle.internal.locking.LockFileReaderWriter.DEPENDENCY_LOCKING_FOLDER and FILE_SUFFIX
             * have package scope
             * <grv87 2018-07-08>
             */
              !project.file("gradle/dependency-locks/${ configuration.name }.lockfile").exists()
          }
          null
        }
        taskProviders.get(INSTALL, type).configure { Task task -> task.dependsOn installTaskProvider }

        TaskProvider<ResolveAndLockTask> updateTaskProvider = project.tasks.register(GRADLE_TASK_NAME(UPDATE, type), ResolveAndLockTask) { ResolveAndLockTask resolveAndLockTask ->
          resolveAndLockTask.configurationMatcher = { Configuration configuration ->
            PrerequisiteType.fromConfigurationName(configuration.name) == type
          }
          null
        }
        taskProviders.get(UPDATE, type).configure { Task task -> task.dependsOn updateTaskProvider }
      }
    }

    project.plugins.withId('nebula.dependency-lock') {
      Class<? extends Task> abstractLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.AbstractLockTask')
      Class<? extends Task> generateLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.GenerateLockTask')
      Class<? extends Task> updateLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.UpdateLockTask')
      Class<? extends Task> saveLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.SaveLockTask')
      Class<? extends Task> commitLockTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('nebula.plugin.dependencylock.tasks.CommitLockTask')

      project.tasks.withType(abstractLockTaskClass).configureEach { Task abstractLock ->
        abstractLock.group = null
        null
      }

      taskProviders.get(INSTALL, PREREQUISITY).configure { Task task -> task.dependsOn project.tasks.withType(generateLockTaskClass) }
      taskProviders.get(UPDATE, PREREQUISITY).configure { Task task ->
        task.with {
          dependsOn project.tasks.withType(updateLockTaskClass)
          dependsOn project.tasks.withType(saveLockTaskClass)
          if (((File) project.tasks.withType(saveLockTaskClass).findByName('saveGlobalLock')?.property('outputLock'))?.exists()) {
            dependsOn project.tasks.withType(saveLockTaskClass).named('saveGlobalLock')
          } else {
            dependsOn project.tasks.withType(saveLockTaskClass).named('saveLock')
          }
        }
        null
      }
      project.tasks.withType(saveLockTaskClass).configureEach { Task saveLock ->
        switch (saveLock.name) {
          case 'saveGlobalLock':
            saveLock.dependsOn project.tasks.withType(updateLockTaskClass).named('updateGlobalLock')
            break
          case 'saveLock':
            saveLock.dependsOn project.tasks.withType(updateLockTaskClass).named('updateLock')
            break
          default:
            project.logger.error('org.fidata.prerequisites: unknown task {} of type SaveLockTask', saveLock.name)
        }
        null
      }
      project.tasks.withType(commitLockTaskClass).configureEach { Task commitLock ->
        commitLock.enabled = false
        null
      }
    }

    project.plugins.withId('org.ajoberstar.stutter') {
      project.plugins.withId('java') {
        TaskProvider<Task> stutterWriteLocksProvider = project.tasks.named('stutterWriteLocks')
        taskProviders.get(UPDATE, BUILD_TOOL).configure { Task task -> task.dependsOn stutterWriteLocksProvider }
        stutterWriteLocksProvider.configure { Task stutterWriteLocks ->
          stutterWriteLocks.group = null
          null
        }

        TaskProvider<Task> stutterWriteLocksIfNotExistProvider = project.tasks.register(STUTTER_WRITE_LOCKS_IF_NOT_EXIST_TASK_NAME) { Task task ->
          task.with {
            description = 'Generate lock files of Gradle versions to test for compatibility if they not already exist.'
            onlyIf {
              project.fileTree(dir: ((DirectoryProperty) project.extensions.getByName('stutter').properties['lockDir']).get(), includes: ['*.*']).empty
            }
            actions.addAll stutterWriteLocksProvider.get().actions
          }
        }
        taskProviders.get(INSTALL, BUILD_TOOL).configure { Task task -> task.dependsOn stutterWriteLocksIfNotExistProvider }
      }
    }
  }

  private void setupIntegrationForOutdatedTasks() {
    project.plugins.withId('com.github.ben-manes.versions') {
      Class<? extends Task> dependencyUpdatesTaskClass = (Class<? extends Task>)this.class.classLoader.loadClass('com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask')
      taskProviders.get(OUTDATED, PREREQUISITY).configure { Task task -> task.dependsOn project.tasks.withType(dependencyUpdatesTaskClass) }
      project.tasks.withType(dependencyUpdatesTaskClass).configureEach { Task task ->
        task.group = null
        null
      }
    }

    project.plugins.withId('com.ofg.uptodate') {
      TaskProvider<Task> uptodateProvider = project.tasks.named('uptodate')
      taskProviders.get(OUTDATED, PREREQUISITY).configure { Task task -> task.dependsOn uptodateProvider }
      uptodateProvider.configure { Task uptodate ->
        uptodate.group = null
        null
      }
    }
  }
}
