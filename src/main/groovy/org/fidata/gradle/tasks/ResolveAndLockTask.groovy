package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

/**
 * Task to resolve and lock configurations
 */
@CompileStatic
class ResolveAndLockTask extends DefaultTask {
  @Internal
  Closure<Boolean> configurationMatcher = { Configuration configuration -> true }

  @TaskAction
  void resolveAndLock() {
    assert project.gradle.startParameter.writeDependencyLocks

    project.configurations.matching { Configuration configuration ->
      configuration.canBeResolved &&
      /*
       * WORKAROUND:
       * CodeNarc doesn't work with its configuration locked
       * https://github.com/gradle/gradle/issues/5894
       * <grv87 2018-07-08>
       */
      (
        GradleVersion.current() >= GradleVersion.version('4.9-rc-2') ||
        configuration.name != 'codenarc'
      ) &&
      configurationMatcher.call(configuration)
    }.each { Configuration configuration ->
      configuration.resolve()
    }
  }
}
