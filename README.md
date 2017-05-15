[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder)
[![Circle CI](https://circleci.com/gh/h908714124/zerobuilder.svg?style=shield)](https://circleci.com/gh/h908714124/zerobuilder)

# DEPRECATION WARNING

In May 2017, the core `@Updater` functionality of zerobuilder has been split into smaller projects.
These are easier to maintain, generate fewer LOC and have extra features like `Optional` support.

If you are primarily using the `@Updater` annotation, please consider migrating to one of these:

* <https://github.com/h908714124/readable> for immutable classes
* <https://github.com/h908714124/bean-standard> for beans
* <https://github.com/h908714124/auto-builder> for auto-value users

If you are interested in the telescoping pattern that's generated via `@Builder`:
There's no replacement yet, but there's some [work in progress](https://github.com/h908714124/crate).
