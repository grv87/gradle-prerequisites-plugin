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
package org.fidata.gradle.prerequisites

import groovy.transform.CompileStatic
import org.gradle.api.plugins.JavaPlugin
import java.util.regex.Matcher

/**
 * Enum of different prerequisite types
 */
@CompileStatic
enum PrerequisiteType {
  /**
   * Prerequisites in total
   */
  PREREQUISITY('prerequisites'),
  DEPENDENCY('dependencies'),
  BUILD_TOOL('buildTools')

  final String pluralName

  private PrerequisiteType(String pluralName) {
    this.pluralName = pluralName
  }

  /**
   * Determines prerequisite type from Gradle configuration name
   * @param configurationName configuration name
   * @return prerequisite type
   *         null on null configuration name
   */
  static final PrerequisiteType fromConfigurationName(String configurationName) {
    if (!configurationName) {
      return null
    }
    (
      // `org.gradle.java` plugin
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
        JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME,
      ].any { String dependencyConfigurationName ->
        Matcher matcher = (configurationName =~ /(?i)^(.*)$dependencyConfigurationName$/)
        matcher.matches() && !(matcher.group(1) ==~ /(?i).*test/)
      } ||
      [
        // `com.github.jruby-gradle.base` plugin
        'gems',
        // `com.github.jruby-gradle.jar` plugin
        'jrubyJar',
        // `com.github.jruby-gradle.war` plugin
        'jrubyWar',
      ].contains(configurationName)
    ) ? DEPENDENCY : BUILD_TOOL
  }

  /**
   * All possible non-null prerequisite types
   * @return list of Optional<PrerequisiteType>
   */
  static final List<PrerequisiteType> nonTotalValues() { // TODO property (isTotal / Grouping / ...)
    values().findAll { PrerequisiteType type ->
      type != PREREQUISITY
    }
  }
}
