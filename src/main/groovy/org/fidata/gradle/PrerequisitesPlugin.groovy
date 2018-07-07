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

import org.fidata.gradle.utils.PrerequisiteTaskType
import org.fidata.gradle.utils.PrerequisiteType
import org.fidata.gradle.tasks.ResolveAndLockTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import org.gradle.api.artifacts.Configuration
import com.google.common.collect.TreeBasedTable
import com.google.common.collect.Table
import org.gradle.util.GradleVersion
import com.google.common.collect.Ordering
import com.google.common.collect.Comparators

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

  private Table<PrerequisiteTaskType, Optional<PrerequisiteType>, Task> tasks

  /**
   * Gets name of common task
   * @param taskType type of task
   * @param type type of prerequisite
   * @return name of task
   */
  static final TASK_NAME(PrerequisiteTaskType taskType, PrerequisiteType type) {
    "$taskType${ PrerequisiteType.getPluralName(type).capitalize() }"
  }

  private void setupPrerequisitesLifecycleTasks() {
    tasks = TreeBasedTable.create(Ordering.natural(), Comparators.emptiesFirst(Ordering.naturalOrder()) )
    PrerequisiteTaskType.values().each { PrerequisiteTaskType taskType ->
      (PrerequisiteType.values() + [null]).each { PrerequisiteType type ->
        String taskName = TASK_NAME(taskType, type)
        Task task = project.tasks.create(taskName) { Task task ->
          task.with {
            group = TaskConfiguration.GROUP
            description = taskType.description.call(PrerequisiteType.getPluralName(type))
          }
        }
        tasks.put(taskType, Optional.ofNullable(type), task)
        project.logger.debug('org.fidata.prerequisites: {} task created', task)
      }
      tasks.get(taskType, Optional.empty()).dependsOn tasks.row(taskType).findAll { Optional<PrerequisiteType> type, Task _ -> type.present }.values()
      PrerequisiteType.values().each { PrerequisiteType type1 ->
        tasks.get(taskType, Optional.of(type1)).shouldRunAfter(tasks.row(taskType).findAll { Optional<PrerequisiteType> type2, Task _ ->
          type2.present && type2.get() > type1
        }.values())
      }
    }

    project.afterEvaluate {
      ((Collection<Task>)[
        tasks.row(PrerequisiteTaskType.INSTALL),
        tasks.row(PrerequisiteTaskType.OUTDATED)
      ]*.values().flatten()).each { Task runFirstTask ->
        // Recursively get all task direct and indirect dependencies
        Set<Task> excludedTasks = []
        Set<Task> newExcludedTasks = [runFirstTask].toSet()
        while (true) {
          excludedTasks += newExcludedTasks

          Set<Task> veryNewExcludedTasks = []
          newExcludedTasks.each { Task task ->
            veryNewExcludedTasks.addAll((Collection<Task>)task.taskDependencies.getDependencies(task))
            veryNewExcludedTasks.addAll((Collection<Task>)task.mustRunAfter.getDependencies(task))
            veryNewExcludedTasks.addAll((Collection<Task>)task.shouldRunAfter.getDependencies(task))
          }
          veryNewExcludedTasks -= excludedTasks

          if (veryNewExcludedTasks.size() == 0) {
            break
          }

          newExcludedTasks = veryNewExcludedTasks
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
  static final String GRADLE_TASK_NAME(PrerequisiteTaskType taskType, PrerequisiteType type) {
    "${ taskType }Gradle${ PrerequisiteType.getPluralName(type).capitalize() }"
  }
  private void setupIntegrationForInstallUpdateTasks() {
    if (GradleVersion.current() >= GradleVersion.version('4.8')) {
      project.dependencyLocking.lockAllConfigurations()

      PrerequisiteType.values().each { PrerequisiteType type ->
        ResolveAndLockTask installTask = project.tasks.create(GRADLE_TASK_NAME(PrerequisiteTaskType.INSTALL, type), ResolveAndLockTask)
        installTask.configurationMatcher = { Configuration configuration ->
          if (configuration == null) { project.logger.error('configuration is null 2!!!') }
          /*configuration != null &&*/ PrerequisiteType.fromConfigurationName(configuration.name) == type && project.file("gradle/dependency-locks/${ configuration.name }.lockfile").exists() }
        tasks.get(PrerequisiteTaskType.INSTALL, Optional.of(type)).dependsOn installTask

        ResolveAndLockTask updateTask = project.tasks.create(GRADLE_TASK_NAME(PrerequisiteTaskType.UPDATE, type), ResolveAndLockTask)
        updateTask.configurationMatcher = { Configuration configuration ->
          if (configuration == null) { project.logger.error('configuration is null 3!!!') }
          /*configuration != null &&*/ PrerequisiteType.fromConfigurationName(configuration.name) == type }
        tasks.get(PrerequisiteTaskType.UPDATE, Optional.of(type)).dependsOn updateTask
      }
    }

    project.plugins.withId('nebula.dependency-lock') {
      Class<? extends Task> generateLockTaskClass = (Class<? extends Task>)Class.forName('nebula.plugin.dependencylock.tasks.GenerateLockTask', false, this.class.classLoader)
      Class<? extends Task> updateLockTaskClass = (Class<? extends Task>)Class.forName('nebula.plugin.dependencylock.tasks.UpdateLockTask', false, this.class.classLoader)
      Class<? extends Task> saveLockTaskClass = (Class<? extends Task>)Class.forName('nebula.plugin.dependencylock.tasks.SaveLockTask', false, this.class.classLoader)

      project.tasks.withType(generateLockTaskClass) { Task task ->
        task.group = null
        tasks.get(PrerequisiteTaskType.INSTALL, Optional.empty()).mustRunAfter task
      }
      project.tasks.withType(updateLockTaskClass) { Task task ->
        task.group = null
        tasks.get(PrerequisiteTaskType.UPDATE, Optional.empty()).mustRunAfter task
      }
      project.tasks.withType(saveLockTaskClass) { Task task ->
        task.group = null
        tasks.get(PrerequisiteTaskType.UPDATE, Optional.empty()).mustRunAfter task
      }
      project.tasks.findByName('saveGlobalLock')?.with {
        dependsOn project.tasks.getByName('updateGlobalLock')
      }
      project.tasks.getByName('saveLock').with {
        dependsOn project.tasks.getByName('updateLock')
      }
      tasks.get(PrerequisiteTaskType.UPDATE, Optional.empty()).with {
        if (((File)project.tasks.withType(saveLockTaskClass).findByName('saveGlobalLock')?.property('outputLock'))?.exists()) {
          dependsOn project.tasks.withType(saveLockTaskClass).getByName('saveGlobalLock')
        } else {
          dependsOn project.tasks.withType(saveLockTaskClass).getByName('saveLock')
        }
      }
    }
    project.plugins.withId('org.ajoberstar.stutter') {
      project.plugins.withId('java') {
        tasks.get(PrerequisiteTaskType.OUTDATED, Optional.empty()).dependsOn project.tasks.getByName('stutterWriteLocks')
      }
    }
  }

  private void setupIntegrationForOutdatedTasks() {
    project.plugins.withId('com.github.ben-manes.versions') {
      tasks.get(PrerequisiteTaskType.OUTDATED, Optional.empty()).dependsOn project.tasks.withType((Class<? extends Task>)Class.forName('com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask', false, this.class.classLoader))
    }

    project.plugins.withId('com.ofg.uptodate') {
      tasks.get(PrerequisiteTaskType.OUTDATED, Optional.empty()).dependsOn project.tasks.getByName('uptodate')
    }
  }
}
