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
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import java.nio.file.Files

/**
 * Specification for {@link PrerequisitesPlugin} class
 * for updating Gradle dependencies
 */
class PrerequisitesPluginSpec extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = Files.createTempDirectory('compatTest').toFile()

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {
    new File(testProjectDir, 'build.gradle') << """\
      plugins {
        id 'org.fidata.prerequisites'
      }
    """.stripIndent()
  }

  // run after every feature method
  void cleanup() {
    /*
     * WORKAROUND:
     * Jenkins doesn't set CI environment variable
     * https://issues.jenkins-ci.org/browse/JENKINS-36707
     * <grv87 2018-06-27>
     */
    if (success || System.getenv().with { containsKey('CI') || containsKey('JENKINS_URL') }) {
      testProjectDir.delete()
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'plugin applies without exceptions'() {
    when: 'build is run'
    build()

    then: 'no exception thrown'
    noExceptionThrown()
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
