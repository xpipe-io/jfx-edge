# jfx-openbuild - Your own production JavaFX releases

Fixes and features for JavaFX can take a long time from an open pull request to an official release. Some changes are never merged due to concerns about compatibility or limited review capacity. If your application depends on such a change, or you want to apply some internal changes, your options are usually to wait or to build and maintain JavaFX yourself
on every platform, which requires setting up the native toolchains, code signing, and packaging.
The jfx repository does not contain all the necessary build scripts and documentation for proper releases, so you still had to do a lot the work yourself.

This repository automates everything and gives you a lot of new options as well. You list the upstream base and the PRs, branches, or commits you need,
and GitHub Actions builds, signs, and publishes JavaFX for all platforms in the same formats as the official builds.
It also follows upstream automatically, so your build is rebuilt whenever the base or one of the merged changes is updated.
All of it runs in GitHub actions. Plus, you can configure publishing to a Maven repository as well.

In fact, the artifacts built by this repository are even better than the official releases in some aspects.
The Gluon Linux aarch64 libraries require glibc 2.38, so they don't run on distributions like Debian 12 or RHEL 9,
while the libraries built here only require glibc 2.17 on all Linux platforms.
And the official Windows DLLs are not signed at all in the Oracle and Gluon builds, while the builds here can sign all native libraries with your own certificates.

## Customization

You can easily create your custom JavaFX production builds with this repository:

1. Fork this repository + enable GH actions
2. Create a build config file in [/configs](/configs). You can use the [samples](/configs_sample) as a reference. You can also create multiple configs for multiple builds
3. Customize your builds, see below for the configuration reference
4. Once you created a config, your release should be created after the workflows are done (this might take a while depending on your configuration)

## Build workflow

This repository does not contain JavaFX source itself. For every configuration, its GitHub Actions workflow:

1. Clones the [Base](#base) repository at the configured branch, tag or commit
2. Merges in every pull request / branch / commit listed in the
   [Merge list](#merge-list)
3. Builds the merged tree on the same on all platforms as normal, plus codesigns them if configured
4. Runs the headless test suite (if enabled) and includes the result in the release description. (A failing test does not cancel the build)
5. Publishes the per-platform artifacts as a new [GitHub release](../../releases), see [Artifacts](#artifacts)

The workflow runs on a daily schedule as a cron job, on every push to `master`, and when started manually.
Before doing any work it resolves
the current commit of the base and the current head of every entry
in the merge list, together with the build properties, and compares that against the state
recorded on the latest release of the configuration. If nothing has actually changed, the build does not continue.
Pushes that modify the workflows or scripts always trigger a build.

For a release configuration, only the jfx build tag
of the base is compared, see [Type](#type). A manual run always builds the snapshot configurations.

## Configuration

A configuration is a Markdown file in the [`configs`](configs) directory, e.g. [`configs/nightly.md`](configs_sample/nightly.md).
Its name, e.g. `nightly`, is also the suffix of its version, see [Artifacts](#artifacts), so it may only contain
letters and digits. To add a configuration, copy an existing file and change it.

A configuration contains the following sections, each between a `<!-- NAME-START -->` / `<!-- NAME-END -->` pair of markers,
which are explained below: `TYPE`, `BASE`, `MERGE-LIST`, `BUILD-PROPS`, `PLATFORMS`, `BOOT-JDK`, `RUNNERS`, `TESTS` and `MAVEN-REPOSITORY`.
Any other text in the file is ignored.

### Type

The `TYPE` section of a configuration is either `snapshot` or `release`:

- `snapshot` - built again whenever anything changed: the commits of the base or the merge list, the configuration,
  or the workflows and scripts of this repository. A newer build of the same version replaces the previous one.
- `release` - built once for every new jfx build tag of the upstream base, e.g. `28+15`, from exactly that tag and the merge list.
  Its GitHub release and Maven artifacts are never replaced. Changes to the configuration are only included in the
  build for the next tag.

### Base

The base is the tree that everything else is merged into.
Put exactly one GitHub link between the `BASE-START` / `BASE-END` markers to change it:

- `https://github.com/openjdk/jfx/tree/master` - a branch, e.g. the latest development state
- `https://github.com/openjdk/jfx/tree/25+29` - a tag, e.g. a specific JavaFX release
- `https://github.com/openjdk/jfx25u/tree/master` - another repository, e.g. an update release repository or a fork
- `https://github.com/openjdk/jfx/commit/<sha>` - a fixed commit (use the full SHA)

### Merge list

You can edit the merge list of a configuration to control what gets merged into its build.
Each entry is a GitHub link on its own bullet line, between the
`MERGE-LIST-START` / `MERGE-LIST-END` markers. A script greps this block out
of the configuration with a regex, so keep entries in that exact form.

Supported link forms:

- `https://github.com/openjdk/jfx/pull/1234`
- `https://github.com/someuser/jfx/tree/some-branch`
- `https://github.com/someuser/jfx/commit/<sha>`

You can also include rejected or closed JavaFX PRs.
If an entry fails to fetch or conflicts with the rest of the tree, the run fails.

### Build properties

You can pass extra gradle build properties by listing `KEY=VALUE` pairs (one per line) between the
`BUILD-PROPS-START` / `BUILD-PROPS-END` markers. Each line becomes a `-PKEY=VALUE` flag, passed to gradle on every
platform, for both the build and the test run.

This can for example be:

- `COMPILE_MEDIA=true` - build the native media (audio/video) pipeline
- `COMPILE_WEBKIT=true` - build the native WebView pipeline

## Platforms

Only the platforms listed between the `PLATFORMS-START` / `PLATFORMS-END` markers are built
and published. Supported platforms are:

- `linux-x64`
- `linux-aarch64`
- `macos-x64`
- `macos-aarch64`
- `windows-x64`

### Tests

The tests that are run after the build on every platform are configured between the
`TESTS-START` / `TESTS-END` markers. Failing tests don't cancel the build, they are listed in the release notes.

- `skip` - don't run any tests
- `smoke` - the tests that also run in the CI of openjdk/jfx
- `full` - additionally the system tests. As the build machines have no display, they run on the
  headless platform of JavaFX, and the tests that need AWT are left out

The web tests only run if `COMPILE_WEBKIT=true`.

### Boot JDK

The JDK used to build JavaFX is configured between the `BOOT-JDK-START` / `BOOT-JDK-END` markers,
with the same values that [actions/setup-java](https://github.com/actions/setup-java) accepts:

- `distribution=<name>` - e.g. `temurin`, `zulu`, `corretto` or `oracle`
- `version=<version>` - e.g. `26.0.2` for an exact version, or `26` for the latest update of that release

The boot JDK must satisfy the requirement of the base,
see `jfx.build.jdk.version.min` in its `build.properties`.
It is also used to run the scripts of this repository in all workflows, which need Java 25 or newer.

### Runners

The GitHub Actions runners are configured in the `RUNNERS` section of a configuration, as `<platform>=<runner>`
for every platform and `default=<runner>` for all other jobs. A runner is a label like `ubuntu-26.04`,
or a comma-separated list of labels, e.g. for self-hosted runners like `linux-aarch64=self-hosted, linux, arm64`.

#### Linux glibc compatibility

The Linux native libraries are compiled inside a
[manylinux2014](https://github.com/pypa/manylinux) container (CentOS 7,
glibc 2.17) rather than directly on the runner, so they run on much older
distros than Ubuntu 26.04 (the runner's own glibc) would otherwise allow.

### Maven repository

The Maven artifacts are published to the Maven repository in the `MAVEN-REPOSITORY` section of the configuration, with the same artifacts as on Maven Central, e.g. `org.openjfx:javafx-graphics` with the platform jars
for the classifiers `linux`, `linux-aarch64`, `mac`, `mac-aarch64` and `win`, and the `sources` and `javadoc` jars.
Leave it empty to not publish them.

For a snapshot configuration, a newer build of the same version replaces the artifacts, so they are published as
snapshot of the version, e.g. `28-nightly+14-SNAPSHOT`. A release configuration publishes them with their version,
e.g. `28-stable+15`.

The credentials for publishing are the secrets `MAVEN_USERNAME` and `MAVEN_PASSWORD`. If they aren't set,
the token of the workflow is used, which can publish to the GitHub Packages of this repository.

### GPG signing

You can sign the Maven artifacts with GPG by configuring the following secrets. Every file then gets an `.asc` signature,
in the Maven repository and among the release assets. If `GPG_KEY` isn't set, they are not signed.

| Name               | Purpose                                                  |
|--------------------|----------------------------------------------------------|
| `GPG_KEY`          | the ASCII-armored private key, e.g. from `gpg --armor --export-secret-keys <key id>` |
| `GPG_KEY_ID`       | the id of the key, e.g. `ABCD1234`                       |
| `GPG_KEY_PASSWORD` | the passphrase of the key                                |

### Windows code signing

You can sign the Windows .dlls by configuring the following variables for your Azure Key Vault:

| Name                            | Purpose                                              |
|----------------------------------|-------------------------------------------------------|
| `AZURE_KEY_VAULT_URI`             | e.g. `https://myvault.vault.azure.net/`                |
| `AZURE_TENANT_ID`       | Azure AD tenant id                                     |
| `AZURE_CLIENT_ID`       | service principal client id with sign permission on the vault |
| `AZURE_CLIENT_SECRET`   | service principal client secret                        |
| `AZURE_CERT_NAME`     | name of the certificate in the vault                   |

If `AZURE_KEY_VAULT_URI` isn't set, signing is skipped and the build
continues unsigned.

The signing is done by the scripts in [`scripts/windows`](scripts/windows): `setup_sign.ps1` prepares it, and `sign.cmd`
is called by the jfx build for every native library. To sign in another way, replace these scripts and the
secrets passed to them in [`build-windows.yml`](.github/workflows/build-windows.yml).

### macOS code signing and notarization

You can configure notarization and signing of the native macOS libraries by configuring
the following variables for your Apple developer account and certificate:

| Name                                                     | Purpose                                                    |
|-------------------------------------------------------------|--------------------------------------------------------------|
| `MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE`               | base64-encoded `.p12` Developer ID Application certificate    |
| `MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_PASSWORD`      | password for that `.p12`                                      |
| `MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME`          | e.g. `Developer ID Application: Example Org (TEAMID)`          |
| `MACOS_NOTARIZATION_APPLE_ID`                              | Apple ID used for notarization                                |
| `MACOS_NOTARIZATION_APP_SPECIFIC_PASSWORD`                 | app-specific password for that Apple ID                       |
| `MACOS_NOTARIZATION_TEAM_ID`                               | Apple Developer team id                                       |

If `MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE` isn't set, signing and
notarization are skipped and the build continues unsigned.

The signing is done by the scripts in [`scripts/macos`](scripts/macos): `setup_sign.sh` prepares it, `sign.sh`
is called by the jfx build for every native library, and `notarize.sh` notarizes the signed libraries afterwards.
To sign in another way, replace these scripts and the secrets passed to them in [`build-macos.yml`](.github/workflows/build-macos.yml).

### Artifacts

Every build is published as a release, tagged with its version.
For every platform, the release contains the same bundles as the official JavaFX downloads,
named like them (`<platform>` is one of `linux-x64`, `linux-aarch64`, `osx-x64`, `osx-aarch64` and `windows-x64`):

- `openjfx-<version>_<platform>_bin-sdk.zip` - the JavaFX SDK
- `openjfx-<version>_<platform>_bin-jmods.zip` - the jmods, e.g. for jlink
- `openjfx-<version>_<platform>_bin-maven.zip` - the platform jars of the Maven publication, e.g. `javafx.graphics-linux-aarch64.jar`,
  using the same classifiers as Maven Central

Additionally, it contains the javadoc of all modules as `openjfx-<version>-javadoc.zip`.
The individual files of the Maven publication are not attached, as their hundreds of uploads would exceed
the rate limits of GitHub. They are published to the Maven repository of the configuration, if it has one.

The version is `<version>-<configuration>+<build>`, e.g. `28-nightly+14`, so it can not be confused with an official one,
and every configuration has its own versions. The build number is the one of the closest jfx tag of the base (`28+14`).
This is also what `javafx.runtime.version` reports at runtime.

For a snapshot configuration, the release is tagged with `-SNAPSHOT` like its Maven version, e.g. `28-nightly+14-SNAPSHOT`,
and a newer build with the same version replaces the existing release. If the repository has
[immutable releases](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/immutable-releases)
enabled, releases can't be replaced, so then the tag also contains the time of the build,
e.g. `28-nightly+14-SNAPSHOT-2026-10-24-083551`.

### Release tags

Every GitHub release needs a tag. If the repository has a `releases` branch, all release tags point to its commit,
so that they are not part of the history of `master`. Otherwise they point to the commit that was built, which isn't really useful.
The branch only needs a single commit without any content, which you can create with:

```
git switch --orphan releases
git commit --allow-empty -m "Target of the release tags"
git push origin releases
git switch master
```