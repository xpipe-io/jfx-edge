# ea

Builds of the upstream early access releases, without any changes. A new release is created for every new build tag of jfx master, e.g. `28+15`,
so this is the closest to the official early access builds.

The configuration is a:

<!-- TYPE-START -->
release
<!-- TYPE-END -->

The build is based on:

<!-- BASE-START -->
- https://github.com/openjdk/jfx/tree/master
<!-- BASE-END -->

Nothing is merged into this build:

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
skip
<!-- TESTS-END -->

The Maven artifacts are published to the GitHub Packages of your fork, e.g. `https://maven.pkg.github.com/<owner>/<repository>`.
They are not published to a Maven repository if this is empty:

<!-- MAVEN-REPOSITORY-START -->

<!-- MAVEN-REPOSITORY-END -->
