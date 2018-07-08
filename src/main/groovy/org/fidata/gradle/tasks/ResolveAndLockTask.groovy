package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ResolveAndLockTask extends DefaultTask {
  @Internal
  Closure<Boolean> configurationMatcher = { Configuration configuration -> true }

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
