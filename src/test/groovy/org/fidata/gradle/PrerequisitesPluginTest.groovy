#!/usr/bin/env groovy
/*
 * Unit tests for PrerequisitesPluginTest class
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

import org.junit.runner.RunWith
import org.junit.Test
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName

/**
 * Unit tests for {@link PrerequisitesPluginTest} class
 */
@RunWith(JUnitParamsRunner)
class PrerequisitesPluginTest {
  /**
   * Test method for {@link PrerequisitesPluginTest#isDependencyConfiguration(java.lang.String)}.
   */
  @Test
  @Parameters
  @TestCaseName('{index}: isDependencyConfiguration({0}) == {1}')
  void testIsDependencyConfiguration(final String version, final Boolean expectedResult) {
    assert expectedResult == PrerequisitesPlugin.isDependencyConfiguration(version)
  }

  static Object[][] parametersForTestIsDependencyConfiguration() {
    [
      ['archives',                       false],
      ['codenarc',                       false],
      ['annotationProcessor',            false],
      ['compatTestAnnotationProcessor',  false],
      ['functionalTestCompile',          false],
      ['gradleTestCompileClasspath',     false],
      ['testCompileOnly',                false],
      ['compatTestRuntime',              false],
      ['functionalTestRuntimeClasspath', false],
      ['compile',                        true],
      ['compileClasspath',               true],
      ['compileOnly',                    true],
    ]
  }
}
