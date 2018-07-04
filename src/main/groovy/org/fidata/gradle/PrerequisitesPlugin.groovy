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

import org.fidata.gradle.tasks.ResolveAndLockTask

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import org.gradle.api.artifacts.Configuration
import org.gradle.util.GradleVersion

import java.util.regex.Matcher

/**
 * Provides an environment for a general, language-agnostic project
 */
@CompileStatic
final class PrerequisitesPlugin implements Plugin<Project> {
  /**
   * Minimum supported version of Gradle
   */
  public static final String GRADLE_MINIMUM_SUPPORTED_VERSION = '4.8'

  private Project project

  @SuppressWarnings(['BracesForForLoop', 'UnnecessaryObjectReferences'])
  void apply(Project project) {
    assert GradleVersion.current() >= GradleVersion.version(GRADLE_MINIMUM_SUPPORTED_VERSION) : "Gradle versions before $GRADLE_MINIMUM_SUPPORTED_VERSION are not supported"

    this.project = project

    setupPrerequisitesLifecycleTasks()
    setupGradlePrerequisitesLifecycleTasks()
    setupOutdatedPluginsIntegration()
  }

  /**
   * Name of prerequisitesInstall task
   */
  public static final String PREREQUISITES_INSTALL_TASK_NAME = 'prerequisitesInstall'
  /**
   * Name of prerequisitesUpdate task
   */
  public static final String PREREQUISITES_UPDATE_TASK_NAME = 'prerequisitesUpdate'
  /**
   * Name of prerequisitesOutdated task
   */
  public static final String PREREQUISITES_OUTDATED_TASK_NAME = 'prerequisitesOutdated'

  /**
   * Name of buildToolsInstall task
   */
  public static final String BUILD_TOOLS_INSTALL_TASK_NAME = 'buildToolsInstall'
  /**
   * Name of buildToolsUpdate task
   */
  public static final String BUILD_TOOLS_UPDATE_TASK_NAME = 'buildToolsUpdate'
  /**
   * Name of buildToolsOutdated task
   */
  public static final String BUILD_TOOLS_OUTDATED_TASK_NAME = 'buildToolsOutdated'

  /**
   * Name of dependenciesInstall task
   */
  public static final String DEPENDENCIES_INSTALL_TASK_NAME = 'dependenciesInstall'
  /**
   * Name of dependenciesUpdate task
   */
  public static final String DEPENDENCIES_UPDATE_TASK_NAME = 'dependenciesUpdate'
  /**
   * Name of dependenciesOutdated task
   */
  public static final String DEPENDENCIES_OUTDATED_TASK_NAME = 'dependenciesOutdated'

  private Task prerequisitesInstall
  private Task prerequisitesUpdate
  private Task prerequisitesOutdated
  private Task buildToolsInstall
  private Task buildToolsUpdate
  private Task buildToolsOutdated
  private Task dependenciesInstall
  private Task dependenciesUpdate
  private Task dependenciesOutdated
  private void setupPrerequisitesLifecycleTasks() {
    prerequisitesInstall = project.tasks.create(PREREQUISITES_INSTALL_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Install all prerequisites for build'
      }
    }
    prerequisitesUpdate = project.tasks.create(PREREQUISITES_UPDATE_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Update all prerequisites that support automatic update'
      }
    }
    prerequisitesOutdated = project.tasks.create(PREREQUISITES_OUTDATED_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Show outdated prerequisites'
      }
    }

    buildToolsInstall = project.tasks.create(BUILD_TOOLS_INSTALL_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Install all build tools for build'
      }
    }
    buildToolsUpdate = project.tasks.create(BUILD_TOOLS_UPDATE_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Update all build tools that support automatic update'
      }
    }
    buildToolsOutdated = project.tasks.create(BUILD_TOOLS_OUTDATED_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Show outdated build tools'
      }
    }

    dependenciesInstall = project.tasks.create(DEPENDENCIES_INSTALL_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Install all dependencies for build'
      }
    }
    dependenciesUpdate = project.tasks.create(DEPENDENCIES_UPDATE_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Update all dependencies that support automatic update'
      }
    }
    dependenciesOutdated = project.tasks.create(DEPENDENCIES_OUTDATED_TASK_NAME) { Task task ->
      task.with {
        group = TaskConfiguration.GROUP
        description = 'Show outdated dependencies'
      }
    }

    prerequisitesInstall.dependsOn buildToolsInstall, dependenciesInstall
    prerequisitesUpdate.dependsOn buildToolsUpdate, dependenciesUpdate
    prerequisitesOutdated.dependsOn buildToolsOutdated, dependenciesOutdated
    dependenciesInstall.shouldRunAfter prerequisitesInstall
    dependenciesUpdate.shouldRunAfter buildToolsUpdate
    dependenciesOutdated.shouldRunAfter buildToolsOutdated

    project.afterEvaluate {
      [
        prerequisitesInstall,
        buildToolsInstall,
        dependenciesInstall,
        prerequisitesUpdate,
        dependenciesUpdate,
        buildToolsUpdate
      ].each { Task runFirstTask ->
        for (Task task in
          project.tasks
            - runFirstTask
            - runFirstTask.taskDependencies.getDependencies(runFirstTask)
            - runFirstTask.mustRunAfter.getDependencies(runFirstTask)
            - runFirstTask.shouldRunAfter.getDependencies(runFirstTask)
        ) {
          task.mustRunAfter runFirstTask
        }
      }

    }
  }

  static boolean isDependencyConfiguration(String name) {
    [
      JavaPlugin.COMPILE_CONFIGURATION_NAME,
      JavaPlugin.RUNTIME_CONFIGURATION_NAME,
      JavaPlugin.API_CONFIGURATION_NAME,
      JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
      JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
      JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME,
      JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME,
      JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME,
      JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME,
      JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME
    ].any { String configurationName ->
      Matcher matcher = (name =~ /(?i)^(.*)$configurationName$/)
      matcher.matches() && !(matcher.group(1) ==~ /(?i).*test/)
    }
  }

  /**
   * Name of resolveAndLockBuilsTools task
   */
  public static final String RESOLVE_AND_LOCK_BUILD_TOOLS_TASK_NAME = 'resolveAndLockBuildTools'

  /**
   * Name of resolveAndLockDependencies task
   */
  public static final String RESOLVE_AND_LOCK_DEPENDENCIES_TASK_NAME = 'resolveAndLockDependencies'

  private void setupGradlePrerequisitesLifecycleTasks() {
    project.dependencyLocking.lockAllConfigurations()

    ResolveAndLockTask resolveAndLockBuildTools = project.tasks.create(RESOLVE_AND_LOCK_BUILD_TOOLS_TASK_NAME, ResolveAndLockTask) { ResolveAndLockTask task ->
      task.configurationMatcher = { Configuration configuration -> !isDependencyConfiguration(configuration.name) }
    }
    buildToolsUpdate.dependsOn resolveAndLockBuildTools

    ResolveAndLockTask resolveAndLockDependencies = project.tasks.create(RESOLVE_AND_LOCK_DEPENDENCIES_TASK_NAME, ResolveAndLockTask) { ResolveAndLockTask task ->
      task.with {
        doFirst {
          task.extensions.extraProperties['oldWriteDependencyLocks'] = project.gradle.startParameter.writeDependencyLocks
          project.gradle.startParameter.writeDependencyLocks = true
        }
        configurationMatcher = { Configuration configuration -> isDependencyConfiguration(configuration.name) }
        doLast {
          project.gradle.startParameter.writeDependencyLocks = (boolean)task.extensions.extraProperties['oldWriteDependencyLocks']
        }
      }
    }
    dependenciesUpdate.dependsOn resolveAndLockDependencies
  }

  private void setupOutdatedPluginsIntegration() {
    // com.github.ben-manes.versions plugin
    try {
      prerequisitesOutdated.dependsOn project.tasks.withType((Class<? extends Task>)Class.forName('com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask', false, this.class.classLoader))
    } catch (ClassNotFoundException ignored) {
    }

    // com.ofg.uptodate plugin
    prerequisitesOutdated.dependsOn project.tasks.findByName('uptodate')
  }
}
