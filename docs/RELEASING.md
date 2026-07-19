# Releasing U-API

Release publishing is tag-driven. Normal pushes and pull requests do not publish anything.

## GitHub setup

Create these repository secrets:

- `MODRINTH_TOKEN` — Modrinth API token. This is mandatory.
- `CURSEFORGE_TOKEN` — CurseForge API token. Optional; only needed when publishing to CurseForge.

Create these repository variables:

- `MODRINTH_PROJECT_ID` — U-API project ID or slug on Modrinth. This is mandatory.
- `CURSEFORGE_PROJECT_ID` — U-API project ID on CurseForge. Optional.

Secrets are encrypted and intended for tokens. Variables are non-secret repository configuration such as project IDs.

## Release order for connected mods

When releasing matching U-API and Soul Ascension versions, publish in this order:

1. Release U-API.
2. Verify that U-API is available on Modrinth.
3. Update and test the U-API dependency used by Soul Ascension if needed.
4. Release Soul Ascension.

Do not publish Soul Ascension against a U-API version that is not available to users yet.

## Local dry-run

Run from the repository root:

```powershell
.\scripts\release.ps1 -DryRun
```

Beta and alpha dry-runs:

```powershell
.\scripts\release.ps1 -Channel beta -DryRun
.\scripts\release.ps1 -Channel alpha -DryRun
```

Dry-run performs the same local checks and clean Gradle build, then stops before creating or pushing a tag.

## Normal release

```powershell
git status
git add .
git commit -m "Release 2.0.0"
git push github <branch>
.\scripts\release.ps1
```

Beta:

```powershell
.\scripts\release.ps1 -Channel beta
```

Alpha:

```powershell
.\scripts\release.ps1 -Channel alpha
```

The script creates and pushes an annotated tag only after all checks pass.

## Tag format

Release:

```text
v2.0.0+mc1.21.1
```

Beta/alpha:

```text
v2.0.0-beta+mc1.21.1
v2.0.0-alpha+mc1.21.1
```

The internal `gradle.properties` values remain separate:

```properties
mod_version=2.0.0
minecraft_version=1.21.1
release_remote=github
```

The built user-facing JAR includes the Minecraft suffix, for example:

```text
u-api-2.0.0+mc1.21.1.jar
```

## What the local script checks

- It is running inside this Git repository and from the repository root.
- `gradle.properties` and `gradlew.bat` exist.
- The current branch is not detached.
- The working tree has no modified or untracked files.
- The current branch exists on the configured `release_remote`.
- Local `HEAD` exactly matches `<release_remote>/<branch>`.
- `mod_version`, `minecraft_version` and `mod_name` are readable.
- The target tag does not exist locally or on the configured `release_remote`.
- `.\gradlew.bat clean build` succeeds.

The script never commits source files, pushes branches, rewrites tags, or creates GitHub Releases locally.

## What GitHub Actions does

When the tag is pushed, `.github/workflows/release.yml`:

1. validates that the tag matches `gradle.properties`;
2. validates mandatory Modrinth settings;
3. builds from the tagged commit on GitHub;
4. selects exactly one user-facing JAR from `build/libs`;
5. publishes to Modrinth;
6. publishes to CurseForge if both CurseForge settings are present;
7. creates the GitHub Release and uploads the same JAR;
8. writes a release summary.

## Recovery after errors

If the local build fails, fix the project, commit the fix, push the branch and run the script again.

If the tag push fails, the script removes the local tag automatically. If you need to remove a mistaken tag manually:

```powershell
git tag -d v2.0.0+mc1.21.1
git push github :refs/tags/v2.0.0+mc1.21.1
```

Do not reuse a published tag for a different commit. Users, GitHub Releases and publishing platforms can cache tag state. Prefer increasing the patch version and publishing a new release.

If Modrinth rejects a file, fix the cause, bump the patch version and publish a new tag. The workflow intentionally fails before creating the final GitHub Release when mandatory Modrinth publication is not configured.

## Multiple Minecraft versions

Tags are unique across the entire repository:

```text
v2.0.0+mc1.21.1
v2.0.0+mc1.21.4
```

The workflow only compares previous tags with the same `+mc<version>` suffix when generating fallback release notes.
