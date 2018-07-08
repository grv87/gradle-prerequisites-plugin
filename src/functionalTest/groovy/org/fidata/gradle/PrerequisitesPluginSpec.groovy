#!/usr/bin/env groovy
/*
 * Specification for org.fidata.prerequisites Gradle plugin
 * Copyright © 2018  Basil Peace
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

import org.gradle.api.Task
import spock.lang.Specification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Unroll

/**
 * Specification for {@link org.fidata.gradle.PrerequisitesPlugin} class
 */
class PrerequisitesPluginSpec extends Specification {
  // fields
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  Project project

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {
    project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
  }

  // run after every feature method
  // void cleanup() { }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  @Unroll
  void 'provides #task task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    then: '#task task exists'
    project.tasks.getByName(task)

    where:
    task << [
      'installPrerequisites', 'updatePrerequisites', 'outdatedPrerequisites',
      'installBuildTools',    'updateBuildTools',    'outdatedBuildTools',
      'installDependencies',  'updateDependencies',  'outdatedDependencies',
    ]
  }

  @Unroll
  void 'integrates update task with #pluginId plugin'() {
    given: 'java plugin is applied'
    project.apply plugin: 'java'
    and: '#pluginId plugin is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    then: '#updateTaskName task depends on #pluginTaskName task'
    Task updateTask = project.tasks[updateTaskName]
    Task pluginTask = project.tasks[pluginTaskName]
    updateTask.taskDependencies.getDependencies(updateTask).contains(pluginTask)

    and: '#pluginTaskName task has no group'
    pluginTask.group == null

    where:
    pluginId = 'org.ajoberstar.stutter'
    pluginTaskName = 'stutterWriteLocks'
    prerequisitiesType = 'buildTools'
    updateTaskName = 'update' + prerequisitiesType.capitalize()
  }

  @Unroll
  void 'integrates outdated task with #pluginId plugin'() {
    given: '#pluginId plugin is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    then: '#outdatedTaskName task depends on #pluginTaskName task'
    Task outdatedTask = project.tasks[outdatedTaskName]
    Task pluginTask = project.tasks[pluginTaskName]
    outdatedTask.taskDependencies.getDependencies(outdatedTask).contains(pluginTask)

    and: '#pluginTaskName task has no group'
    pluginTask.group == null

    where:
    pluginId                        | pluginTaskName      | prerequisitiesType
    'com.github.ben-manes.versions' | 'dependencyUpdates' | 'prerequisites'
    'com.ofg.uptodate'              | 'uptodate'          | 'prerequisites'
    outdatedTaskName = 'outdated' + prerequisitiesType.capitalize()
  }

  // helper methods
}
