gradle-prerequisites-plugin
===========================

This plugin provides prerequisites lifecycle to Gradle.

## Definitions and the aim of this plugin

Prerequisites are all external artifacts used by our project.

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

### Types of Prerequisites

There are two types of prerequisites:
*   build tools — prerequisites of build scripts themselves, including
prerequisites for testing, releasing etc.
*   dependencies — prerequisites of produced artifacts

The distinction between them is relevant when we think
in the terms of produced artifacts.
If the prerequisite is used by produced artifact
then the change of prerequisite or version could affect consumers
and so could be [breaking change](https://conventionalcommits.org).
Сhanges in build tools prerequisites generally don't produce breaking
changes (although, there could be exclusions).

## What this plugin provides

### Tasks

This plugin follows Bundler-like style of managing prerequisites.

For each set of dependencies there are three tasks:
*   install — install prerequisites according to locked versions
*   update — update locked versions of prerequisites
*   outdated — list updates for prerequisites versions locked

<table><tboby>
<tr><td>installPrerequisites</td><td>updatePrerequisites</td><td>outdatedPrerequisites</td></tr>
<tr><td>installBuildTools   </td><td>updateBuildTools   </td><td>outdatedBuildTools   </td></tr>
<tr><td>installDependencies </td><td>updateDependencies </td><td>outdatedDependencies </td></tr>
</tboby></table>

### Gradle dependencies
Gradle


### Integration with other plugins

Plugin automatically integrates with the following plugins
if they are applied:

*




------------------------------------------------------------------------
Copyright © 2018  Basil Peace

This file is part of gradle-prerequisites-plugin.

Copying and distribution of this file, with or without modification,
are permitted in any medium without royalty provided the copyright
notice and this notice are preserved.  This file is offered as-is,
without any warranty.
