#!/usr/bin/env groovy
/*
 * PrerequisiteTaskType enum
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

/**
 * Enum of different task types supported by the plugin
 */
@CompileStatic
enum PrerequisiteTaskType {
  INSTALL({ String prerequisiteTypePluralName -> "Install all $prerequisiteTypePluralName for build".toString() }),
  UPDATE({ String prerequisiteTypePluralName -> "Update all $prerequisiteTypePluralName that support automatic update".toString() }),
  OUTDATED({ String prerequisiteTypePluralName -> "Show outdated $prerequisiteTypePluralName".toString() })

  final Closure<String> description
  PrerequisiteTaskType(Closure<String> description) {
    this.description = description
  }

  @Override
  String toString() {
    name().toLowerCase()
  }
}
