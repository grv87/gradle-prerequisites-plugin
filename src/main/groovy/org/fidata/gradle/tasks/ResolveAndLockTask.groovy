package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ResolveAndLockTask extends DefaultTask {
  @Input
  Closure<Boolean> configurationMatcher = { true }

  @TaskAction
  void resolveAndLock() {
    assert project.gradle.startParameter.writeDependencyLocks

    project.configurations.each{ Configuration configuration ->
      if (configuration.canBeResolved && configurationMatcher.call(configuration)) {
        configuration.resolve()
      }
    }
  }
}
