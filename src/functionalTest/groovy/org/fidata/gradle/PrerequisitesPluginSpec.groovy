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
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Specification for {@link PrerequisitesPlugin} class
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
  void 'provides #taskName task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    then: '#taskName task exists'
    project.tasks.getByName(taskName)

    where:
    taskName << [
      'installPrerequisites', 'updatePrerequisites', 'outdatedPrerequisites',
      'installBuildTools',    'updateBuildTools',    'outdatedBuildTools',
      'installDependencies',  'updateDependencies',  'outdatedDependencies',
    ]
  }

  @Unroll
  void 'integrates with nebula.dependency-lock plugin'() {
    given: '#pluginId plugin is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    and: 'project evaluated'
    project.evaluate()

    then: '#taskName task depends on #pluginTaskName task'
    Task task = project.tasks[taskName]
    Task pluginTask = project.tasks[pluginTaskName]
    task.taskDependencies.getDependencies(task).contains(pluginTask)

    and: '#pluginTaskName task has no group'
    pluginTask.group == null

    where:
    pluginId = 'nebula.dependency-lock'
    pluginTaskName       | taskName               | depends
    'generateLock'       | 'installPrerequisites' | true
    'updateLock'         | 'saveLock'             | true
    'updateGlobalLock'   | 'saveGlobalLock'       | true
    'saveLock'           | 'updatePrerequisites'  | true
    'saveGlobalLock'     | 'updatePrerequisites'  | false
  }

  @Unroll
  void 'integrates with nebula.dependency-lock plugin with global lock'() {
    given: 'global lock file exists'
    testProjectDir.newFile('global.lock').createNewFile()

    and: '#pluginId plugin is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    and: 'project evaluated'
    project.evaluate()

    then: '#taskName task depends on #pluginTaskName task'
    Task task = project.tasks[taskName]
    Task pluginTask = project.tasks[pluginTaskName]
    task.taskDependencies.getDependencies(task).contains(pluginTask)

    and: '#pluginTaskName task has no group'
    pluginTask.group == null

    where:
    pluginId = 'nebula.dependency-lock'
    pluginTaskName       | taskName              | depends
    'saveLock'           | 'updatePrerequisites' | false
    'saveGlobalLock'     | 'updatePrerequisites' | true
  }

  @Unroll
  void 'integrates with nebula.dependency-lock plugin turning off #pluginTaskName task'() {
    given: '#pluginId plugin is applied'
    project.apply plugin: pluginId
    and: 'nebula.gradle-scm plugin is applied'
    project.apply plugin: 'nebula.gradle-scm'

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    and: 'project evaluated'
    project.evaluate()

    then: '#pluginTaskName task is disabled'
    Task pluginTask = project.tasks[pluginTaskName]
    !pluginTask.enabled

    and: '#pluginTaskName task has no group'
    pluginTask.group == null

    where:
    pluginId = 'nebula.dependency-lock'
    pluginTaskName = 'commitLock'
  }

  @Unroll
  void 'integrates with org.ajoberstar.stutter plugin'() {
    given: 'java plugin is applied'
    project.apply plugin: 'java'
    and: '#pluginId plugin is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    and: 'project evaluated'
    project.evaluate()

    then: '#taskName task depends on #pluginTaskName task'
    Task task = project.tasks[taskName]
    Task pluginTask = project.tasks[pluginTaskName]
    task.taskDependencies.getDependencies(task).contains(pluginTask)

    and: '#pluginTaskName task has no group'
    pluginTask.group == null

    where:
    pluginId = 'org.ajoberstar.stutter'
    pluginTaskName                | taskType
    'stutterWriteLocksIfNotExist' | 'install'
    'stutterWriteLocks'           | 'update'
    prerequisitiesType = 'buildTools'
    taskName = taskType + prerequisitiesType.capitalize()
  }

  @Unroll
  void 'integrates with #pluginId plugin'() {
    given: '#pluginId plugin is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    and: 'project evaluated'
    project.evaluate()

    then: '#taskName task depends on #pluginTaskName task'
    Task task = project.tasks[taskName]
    Task pluginTask = project.tasks[pluginTaskName]
    task.taskDependencies.getDependencies(task).contains(pluginTask)

    and: '#taskName task has no group'
    pluginTask.group == null

    where:
    pluginId                        | pluginTaskName      | taskType   | prerequisitiesType
    'com.github.ben-manes.versions' | 'dependencyUpdates' | 'outdated' | 'prerequisites'
    'com.ofg.uptodate'              | 'uptodate'          | 'outdated' | 'prerequisites'
    taskName = taskType + prerequisitiesType.capitalize()
  }

  // helper methods
}
