#!/usr/bin/env groovy
/*
 * Specification for org.fidata.prerequisites Gradle plugin
 * for integration with org.ajobestar.stutter plugin
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

import spock.lang.IgnoreIf
import org.gradle.util.GradleVersion
import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import java.nio.file.Files
import org.apache.commons.io.FileUtils

/**
 * Specification for {@link PrerequisitesPlugin} class
 * for integration with org.ajobestar.stutter plugin
 */
@IgnoreIf({ GradleVersion.version(System.getProperty('compat.gradle.version')) < GradleVersion.version('4.3') })
class PrerequisitesPluginStutterPluginIntegrationSpec extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = Files.createTempDirectory('compatTest').toFile()

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  // void setup() { }

  // run after every feature method
  void cleanup() {
    /*
     * WORKAROUND:
     * Jenkins doesn't set CI environment variable
     * https://issues.jenkins-ci.org/browse/JENKINS-36707
     * <grv87 2018-06-27>
     */
    if (success || System.getenv().with { containsKey('CI') || containsKey('JENKINS_URL') }) {
      FileUtils.deleteDirectory(testProjectDir)
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'provides stutterWriteLocksIfNotExist task'() {
    given: 'org.ajoberstar.stutter plugin is applied'
    and: 'plugin is applied'
    File buildFile = new File(testProjectDir, 'build.gradle')
    buildFile << '''\
      plugins {
        id 'java'
        id 'org.ajoberstar.stutter' version '0.4.0'
        id 'org.fidata.prerequisites'
      }
      stutter {
        java(8) {
          compatibleRange '4.8'
        }
      }
    '''.stripIndent()

    when: 'build is run'
    build('stutterWriteLocksIfNotExist')

    then: 'stutter lock file is generated'
    File stutterLockFile = new File(testProjectDir, '.stutter/java8.lock')
    stutterLockFile.exists()

    and: 'stutter lock file contains requested Gradle version'
    stutterLockFile.text.split().findAll { !it.startsWith('#') }.contains('4.8')

    when: 'stutter compatibleRange is updated'
    buildFile << '''\
      stutter {
        java(8) {
          compatibleRange '4.0'
        }
      }
    '''.stripIndent()

    and: 'build is run'
    build('stutterWriteLocksIfNotExist')

    then: 'stutter lock file does not contains new requested Gradle version'
    !stutterLockFile.text.split().findAll { !it.startsWith('#') }.contains('4.0')

    (success = true) != null
  }

  // helper methods
  protected BuildResult build(String... arguments) {
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments([*arguments, '--debug', '--full-stacktrace', '--refresh-dependencies'])
      .withDebug(true)
      .forwardOutput()
      .withPluginClasspath()
      .build()
  }

}
