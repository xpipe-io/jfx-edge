# prcheck

A quick check of pull requests: builds them on Linux only and runs the full headless test suite,
without publishing any Maven artifacts. Add more platforms if a change is platform specific.

The configuration is a:

<!-- TYPE-START -->
snapshot
<!-- TYPE-END -->

The build is based on:

<!-- BASE-START -->
- https://github.com/openjdk/jfx/tree/master
<!-- BASE-END -->

The following PRs are checked:

<!-- MERGE-LIST-START -->
- https://github.com/openjdk/jfx/pull/2352
<!-- MERGE-LIST-END -->

The artifacts are built with the following additional properties:

<!-- BUILD-PROPS-START -->
- COMPILE_MEDIA=false
- COMPILE_WEBKIT=false
<!-- BUILD-PROPS-END -->

The following platforms are built:

<!-- PLATFORMS-START -->
- linux-x64
<!-- PLATFORMS-END -->

The following boot JDK is used for the build:

<!-- BOOT-JDK-START -->
- distribution=temurin
- version=26.0.2
<!-- BOOT-JDK-END -->

The following runners are used for the build:

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
full
<!-- TESTS-END -->

No Maven artifacts are published:

<!-- MAVEN-REPOSITORY-START -->

<!-- MAVEN-REPOSITORY-END -->
