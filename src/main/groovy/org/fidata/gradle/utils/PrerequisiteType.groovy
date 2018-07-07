#!/usr/bin/env groovy
/*
 * PrerequisiteType enum
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
package org.fidata.gradle.utils

import groovy.transform.CompileStatic
import org.gradle.api.plugins.JavaPlugin
import sun.awt.image.BufferedImageDevice

import java.util.regex.Matcher

@CompileStatic
public enum PrerequisiteType {
  DEPENDENCY ('dependencies'),
  BUILD_TOOL ('buildTools');

  private final String pluralName

  private PrerequisiteType(pluralName) {
    this.pluralName = pluralName
  }

  /**
   * Plural name of the prerequisite type that can be used for task names
   * @param type prerequisite type, null for prerequisites
   * @return plural name
   */
  static final String getPluralName(PrerequisiteType type) {
    type == null ? 'prerequisites' : type.pluralName
  }

  /**
   * Gets prerequisite type from Gradle configuration name
   * @param configurationName configuration name
   * @return prerequisite type
   *         null on null configuration name
   */

  static PrerequisiteType fromConfigurationName(String configurationName) {
    if (!configurationName) {
      return null
    }
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
    ].any { String dependencyConfigurationName ->
      Matcher matcher = (configurationName =~ /(?i)^(.*)$dependencyConfigurationName$/)
      matcher.matches() && !(matcher.group(1) ==~ /(?i).*test/)
    } ? DEPENDENCY : BUILD_TOOL
  }
}
