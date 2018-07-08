package org.fidata.gradle.prerequisites

enum PrerequisiteTaskType {
  INSTALL({ String prerequisiteTypePluralName -> "Install all $prerequisiteTypePluralName for build" }),
  UPDATE({ String prerequisiteTypePluralName -> "Update all $prerequisiteTypePluralName that support automatic update" }),
  OUTDATED({ String prerequisiteTypePluralName -> "Show outdated $prerequisiteTypePluralName" })

  final Closure<String> description
  PrerequisiteTaskType(Closure<String> description) {
    this.description = description
  }

  @Override
  String toString() {
    name().toLowerCase()
  }
}
