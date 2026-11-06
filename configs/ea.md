# eabuild

The latest early access build of JavaFX, built from its jfx build tag, e.g. `28+14`, without any changes.

The configuration is a:

<!-- TYPE-START -->
release
<!-- TYPE-END -->

The build is based on:

<!-- BASE-START -->
- https://github.com/openjdk/jfx/tree/master
<!-- BASE-END -->

The following PRs and commits are currently merged into this build:

<!-- MERGE-LIST-START -->
<!-- MERGE-LIST-END -->

The artifacts are built with the following additional properties:

<!-- BUILD-PROPS-START -->
- COMPILE_MEDIA=false
- COMPILE_WEBKIT=false
<!-- BUILD-PROPS-END -->

The following platforms are built:

<!-- PLATFORMS-START -->
- linux-x64
- linux-aarch64
- macos-x64
- macos-aarch64
- windows-x64
<!-- PLATFORMS-END -->

The following boot JDK is used for the build:

<!-- BOOT-JDK-START -->
- distribution=temurin
- version=26.0.2
<!-- BOOT-JDK-END -->

The builds run on the following GitHub Actions runners:

<!-- RUNNERS-START -->
- default=ubuntu-26.04
- linux-x64=ubuntu-26.04
- linux-aarch64=ubuntu-26.04-arm
- macos-x64=macos-26-intel
- macos-aarch64=macos-26
- windows-x64=windows-2025
<!-- RUNNERS-END -->

The following tests are run:

<!-- TESTS-START -->
skip
<!-- TESTS-END -->

<!-- MAVEN-REPOSITORY-START -->
<!-- MAVEN-REPOSITORY-END -->
