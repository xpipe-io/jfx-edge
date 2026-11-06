# update27

The JavaFX 27 update releases with additional fixes backported into them.
A new release is created for every new build tag of the update repository, e.g. `27.0.1+5`.

The configuration is a:

<!-- TYPE-START -->
release
<!-- TYPE-END -->

The build is based on:

<!-- BASE-START -->
- https://github.com/openjdk/jfx27u/tree/master
<!-- BASE-END -->

The following fixes are merged into every update release. An entry can be a pull request, a branch,
or a commit, e.g. `https://github.com/openjdk/jfx/commit/<sha>` for a fix from jfx master:

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
- version=25
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
