gradle-prerequisites-plugin
===========================

This plugin provides prerequisites lifecycle to Gradle.

## Definitions and the aim of this plugin

Prerequisites are all external artifacts used by the project.

Some artifacts should be preinstalled.
For example, Gradle requires JRE to run under, and even Gradle Wrapper
doesn't have an ability to download and install it for you.

Other artifacts could be installed automatically during build.

If you are using Gradle only, then you probably don't need this plugin.
Gradle can manage Java (Maven and Ivy) dependencies by itself.

But if you use Gradle as main build tool for multi-language project
and use different tools to install packages for different languages
like [Bundler](https://bundler.io/), [NPM](https://npmjs.com/)
or [Pipenv](https://pipenv.org/) then this plugin could help you
to combine themselves into one lifecycle.

In the future we will have separate plugins for Bundler, NPM, Pipenv
and maybe some more, which will be able to use
generic prerequisites lifecycle provided by this plugin.

### Types of prerequisites

There are two types of prerequisites:
*   build tools — prerequisites of build scripts themselves, including
prerequisites for testing, releasing etc.
*   dependencies — prerequisites of produced artifacts

The distinction between them is relevant when we think
in the terms of produced artifacts.
If the prerequisite is used by produced artifact
then the change of prerequisite or version could affect consumers and so
could introduce [breaking change](https://conventionalcommits.org).
Сhanges in build tools prerequisites generally don't produce breaking
changes (although, there could be exclusions).

## What this plugin provides

### Tasks

The plugin follows Bundler-like style of managing prerequisites.

For each set of prerequisites there are three tasks:

*   `install` — locks prerequisites' versions if they are not
    already locked, then downloads and installs locked versions.
    Note that for tools that could download prerequisites dynamically,
    like Gradle itself, downloading and installation is not performed.
    This should be run once at the start of new project
    or after repository clone

*   `update` — does the same as `install` except that it also updates
    locked versions

*   `outdated` — prints all prerequisites that have
    more recent versions available than locked

So, there are 9 tasks in total:

<table><tboby>
<tr><td>installPrerequisites</td><td>updatePrerequisites</td><td>outdatedPrerequisites</td></tr>
<tr><td>installBuildTools   </td><td>updateBuildTools   </td><td>outdatedBuildTools   </td></tr>
<tr><td>installDependencies </td><td>updateDependencies </td><td>outdatedDependencies </td></tr>
</tboby></table>

### Gradle >= 4.8 dependency locking

For Gradle >= 4.8 plugin integrates with
[built-in dependency locking mechanism](
https://docs.gradle.org/4.8/userguide/dependency_locking.html),
no extra setup is needed.

This lead to the following two requirements for running
`install` and `update` tasks with Gradle >= 4.8:
1.  They should be run with `--write-locks` argument.
2.  They should be run separately from all other tasks,
    otherwise all other configurations resolved during the run
    would also get their configurations updated and locked.

There is an inconsistency: `buildSrc` project belongs in whole
to build tools, but running `updateDependencies`
updates `buildSrc` dependencies too.
There is no known way to overcome it.

Note that `annotationProcessor` configurations are considered as build
tools, although, there could be a situation where annotation processor
generates some API, and change of processor version changes
that public API.
If this is your case, please, [report an issue](
https://github.com/FIDATA/gradle-prerequisites-plugin/issues/new).

### Integration with other plugins

Plugin automatically integrates with the following plugins
if they are applied.
Some plugins don't provide a way to separate prerequisites to
build tools and dependencies, such integration is called *generic*.

| Plugin | Integration | Notes |
| ------ | ----------- | ----- |
| [`nebula.dependency-lock`](https://github.com/nebula-plugins/gradle-dependency-lock-plugin) | generic | Full integration is possible, but not implemented |
| [`com.github.jruby-gradle`](http://jruby-gradle.org/) set of plugins                        | full    |                                                   |
| [`org.ajoberstar.stutter`]([https://github.com/ajoberstar/gradle-stutter)                   | full    |                                                   |
| [`com.github.ben-manes.versions`](https://github.com/ben-manes/gradle-versions-plugin)      | generic |                                                   |
| [`com.ofg.uptodate`](https://github.com/4finance/uptodate-gradle-plugin)                    | generic |                                                   |

Offerings and pull requests to support other plugins are appreciated.

## Compatibility

*   Tested with Gradle >= 2.10, but it is probably working
    with previous versions too
*   JDK 8


------------------------------------------------------------------------
Copyright © 2018  Basil Peace

This file is part of gradle-prerequisites-plugin.

Copying and distribution of this file, with or without modification,
are permitted in any medium without royalty provided the copyright
notice and this notice are preserved.  This file is offered as-is,
without any warranty.
