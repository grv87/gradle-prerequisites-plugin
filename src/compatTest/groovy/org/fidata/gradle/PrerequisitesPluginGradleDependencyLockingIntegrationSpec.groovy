#!/usr/bin/env groovy
/*
 * Specification for org.fidata.prerequisites Gradle plugin
 * for integration with Gradle dependency locking
 * Copyright ©  Basil Peace
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

import spock.lang.IgnoreIf
import org.gradle.util.GradleVersion
import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll
import org.gradle.testkit.runner.UnexpectedBuildFailure

/**
 * Specification for {@link PrerequisitesPlugin} class
 * for integration with Gradle dependency locking
 */
@IgnoreIf({ GradleVersion.version(System.getProperty('compat.gradle.version')) < GradleVersion.version('4.8') })
class PrerequisitesPluginGradleDependencyLockingIntegrationSpec extends Specification {
  // fields
  boolean success = false

  final File repoDir = File.createTempDir('compatTest', '-repo')
  final File dependeeProjectDir = File.createTempDir('compatTest', '-dependee-project')
  final File testProjectDir = File.createTempDir('compatTest', '-project')

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature methodg
  void setup() {
    releaseDependee '1.0'

    new File(testProjectDir, 'build.gradle') << """\
      plugins {
        id 'java'
        id 'org.fidata.prerequisites'
      }

      repositories {
        maven {
          url ${ repoDir.toString().inspect() }
        }
      }

      dependencies {
        compile 'com.example:dependee:latest.release'
        testCompile 'com.example:dependee:latest.release'
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
      repoDir.deleteDir()
      dependeeProjectDir.deleteDir()
      testProjectDir.deleteDir()
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'locks build tools configurations'() {
    when: 'installBuildTools --write-locks is run'
    build 'installBuildTools', '--write-locks'

    then: 'testCompile configuration contains dependee 1.0 locked version'
    new File(testProjectDir, 'gradle/dependency-locks/testCompile.lockfile').readLines().contains 'com.example:dependee:1.0'

    and: 'compile configuration is not locked'
    !(new File(testProjectDir, 'gradle/dependency-locks/compile.lockfile').exists())

    (success = true) != null
  }

  void 'locks dependencies configurations'() {
    when: 'installDependencies is run'
    build 'installDependencies', '--write-locks'

    then: 'compile configuration contains dependee 1.0 locked version'
    new File(testProjectDir, 'gradle/dependency-locks/compile.lockfile').readLines().contains 'com.example:dependee:1.0'

    and: 'testCompile configuration is not locked'
    !(new File(testProjectDir, 'gradle/dependency-locks/testCompile.lockfile').exists())

    (success = true) != null
  }

  void 'updates build tools configurations'() {
    given: 'all prerequisites are locked'
    build 'installPrerequisites', '--write-locks'

    and: 'there is 2.0 dependee version'
    releaseDependee '2.0'

    when: 'buildToolsUpdate --write-locks is run'
    build 'updateBuildTools', '--write-locks'

    then: 'testCompile configuration contains dependee 2.0 locked version'
    new File(testProjectDir, 'gradle/dependency-locks/testCompile.lockfile').readLines().contains 'com.example:dependee:2.0'
    and: 'compile configuration does not contain dependee 2.0 locked version'
    !(new File(testProjectDir, 'gradle/dependency-locks/compile.lockfile').readLines().contains('com.example:dependee:2.0'))

    (success = true) != null
  }

  void 'updates dependencies configurations'() {
    given: 'all prerequisites are locked'
    build 'installPrerequisites', '--write-locks'

    and: 'there is 2.0 dependee version'
    releaseDependee '2.0'

    when: 'dependenciesUpdate is run'
    build 'updateDependencies', '--write-locks'

    then: 'compile configuration contains dependee 2.0 locked version'
    new File(testProjectDir, 'gradle/dependency-locks/compile.lockfile').readLines().contains 'com.example:dependee:2.0'
    and: 'testCompile configuration does not contain dependee 2.0 locked version'
    !(new File(testProjectDir, 'gradle/dependency-locks/testCompile.lockfile').readLines().contains('com.example:dependee:2.0'))

    (success = true) != null
  }

  @Unroll
  void 'fails when #taskName run without --write-locks argument'() {
    when: '#taskName is run'
    build taskName

    then: 'build is failed'
    thrown(UnexpectedBuildFailure)

    (success = true) != null

    where:
    taskName << [
      'installPrerequisites', 'updatePrerequisites',
      'installBuildTools',    'updateBuildTools',
      'installDependencies',  'updateDependencies',
    ]
  }

  // helper methods
  protected BuildResult build(String... arguments) {
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments(arguments.toList() + (System.hasProperty('org.gradle.logging.level') ? [] : ['--info']) + ['--full-stacktrace', '--refresh-dependencies']) // '--no-daemon'
      .withDebug(true)
      .forwardOutput()
      .withPluginClasspath()
      .build()
  }

  protected void releaseDependee(String version) {
    new File(dependeeProjectDir, 'build.gradle').text = """\
      plugins {
        id 'java'
        id 'maven-publish'
      }
      group = 'com.example'
      version = '$version'
      publishing {
        repositories {
          maven {
            url ${ repoDir.toString().inspect() }
          }
        }
        publications {
          mavenJava(MavenPublication) {
            from components.java
          }
        }
      }
    """.stripIndent()
    new File(dependeeProjectDir, 'settings.gradle').text = '''\
      rootProject.name = 'dependee'
    '''.stripIndent()
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(dependeeProjectDir)
      .withArguments(['publish', '--full-stacktrace', '--refresh-dependencies'])
      .forwardOutput()
      .build()
  }

}
