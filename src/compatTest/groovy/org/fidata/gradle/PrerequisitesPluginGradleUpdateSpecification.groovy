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

import spock.lang.Specification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import java.nio.file.Files
import spock.lang.Unroll

/**
 * Specification for {@link org.fidata.gradle.PrerequisitesPlugin} class
 * for updating Gradle dependencies
 */
class PrerequisitesPluginGradleUpdateSpecification extends Specification {
  // fields
  @Rule
  final TemporaryFolder repoDir = new TemporaryFolder()
  @Rule
  final TemporaryFolder dependeeProjectDir = new TemporaryFolder()
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  Project dependeeProject = ProjectBuilder.builder().withProjectDir(dependeeProjectDir.root).build()
  Project project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {

    project =
      buildFile << '''\
      plugins {
        id 'org.fidata.plugin'
      }
    '''.stripIndent()

    settingsFile << '''\
      enableFeaturePreview('STABLE_PUBLISHING')
    '''.stripIndent()

    propertiesFile.withPrintWriter { PrintWriter printWriter ->
      EXTRA_PROPERTIES.each { String key, String value ->
        printWriter.println "$key=$value"
      }
    }
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
      'prerequisitesInstall', 'prerequisitesUpdate', 'prerequisitesOutdated',
      'dependenciesInstall', 'dependenciesUpdate',
      'buildToolsInstall', 'buildToolsUpdate',
    ]
  }

  @Unroll
  void 'integrates update task with #pluginId'() {
    given: '#pluginId is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    then: '#updateTaskName task depends on pluginTaskName task'
    Task updateTask = project.tasks[updateTaskName]
    updateTask.taskDependencies.getDependencies(updateTask).contains(project.tasks[pluginTaskName])

    where:
    pluginId                 | pluginTaskName      | prerequisitiesType
    'org.ajoberstar.stutter' | 'stutterWriteLocks' | 'buildTools'
    updateTaskName = prerequisitiesType + "Update"
  }

  @Unroll
  void 'integrates outdated task with #pluginId'() {
    given: '#pluginId is applied'
    project.apply plugin: pluginId

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.prerequisites'

    then: '#outdatedTaskName task depends on pluginTaskName task'
    Task outdatedTask = project.tasks[outdatedTaskName]
    outdatedTask.taskDependencies.getDependencies(outdatedTask).contains(project.tasks[pluginTaskName])

    where:
    pluginId                        | pluginTaskName      | prerequisitiesType
    'com.github.ben-manes.versions' | 'dependencyUpdates' | 'prerequisites'
    'com.ofg.uptodate'              | 'uptodate'          | 'prerequisites'
    outdatedTaskName = prerequisitiesType + "Outdated"
  }

  // helper methods
  protected BuildResult build(String... arguments) {
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments([*arguments, '--stacktrace', '--refresh-dependencies'])
      .withPluginClasspath()
      .build()
  }
}
