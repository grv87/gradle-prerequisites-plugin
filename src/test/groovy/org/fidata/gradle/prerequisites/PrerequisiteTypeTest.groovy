#!/usr/bin/env groovy
/*
 * Unit tests for PrerequisiteType class
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
package org.fidata.gradle.prerequisites

import org.junit.runner.RunWith
import org.junit.Test
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName

/**
 * Unit tests for {@link PrerequisiteType} class
 */
@RunWith(JUnitParamsRunner)
class PrerequisiteTypeTest {
  /**
   * Test method for {@link PrerequisiteType#fromConfigurationName(java.lang.String)}.
   */
  @Test
  @Parameters
  @TestCaseName('{index}: fromConfigurationName({0}) == {1}')
  void testFromConfigurationName(final String configurationName, final PrerequisiteType expectedResult) {
    assert expectedResult == PrerequisiteType.fromConfigurationName(configurationName)
  }

  static Object[][] parametersForTestFromConfigurationName() {
    [
      // `org.gradle.java` plugin
      ['archives',                       PrerequisiteType.BUILD_TOOL],
      ['codenarc',                       PrerequisiteType.BUILD_TOOL],
      ['annotationProcessor',            PrerequisiteType.BUILD_TOOL],
      ['compatTestAnnotationProcessor',  PrerequisiteType.BUILD_TOOL],
      ['functionalTestCompile',          PrerequisiteType.BUILD_TOOL],
      ['gradleTestCompileClasspath',     PrerequisiteType.BUILD_TOOL],
      ['testCompileOnly',                PrerequisiteType.BUILD_TOOL],
      ['compatTestRuntime',              PrerequisiteType.BUILD_TOOL],
      ['functionalTestRuntimeClasspath', PrerequisiteType.BUILD_TOOL],
      ['compile',                        PrerequisiteType.DEPENDENCY],
      ['compileClasspath',               PrerequisiteType.DEPENDENCY],
      ['compileOnly',                    PrerequisiteType.DEPENDENCY],
      // `com.github.jruby-gradle.base` plugin
      ['jrubyExec',                      PrerequisiteType.BUILD_TOOL],
      ['gems',                           PrerequisiteType.DEPENDENCY],
      // `com.github.jruby-gradle.jar` plugin
      ['jrubyJar',                       PrerequisiteType.DEPENDENCY],
      // `com.github.jruby-gradle.war` plugin
      ['jrubyWar',                       PrerequisiteType.DEPENDENCY],
    ]
  }
}
