# Releasing U-API

The release workflow publishes one tagged build to GitHub Releases and Modrinth. CurseForge is
also published when both optional CurseForge settings are present.

## Repository settings

Create these GitHub Actions settings before tagging:

- secret `MODRINTH_TOKEN`;
- variable `MODRINTH_PROJECT_ID`;
- optional secret `CURSEFORGE_TOKEN` and variable `CURSEFORGE_PROJECT_ID`.

## Branch and release

```text
git switch port/26.2
git add -A
git commit -m "Port U-API to Minecraft 26.2"
git push -u github port/26.2
```

Open a pull request into `master` and wait for the `CI` workflow. After merging, switch to the
merged commit and perform a local release dry-run:

```text
git switch master
git pull github master
.\scripts\release.ps1 -DryRun
```

Then run `.\scripts\release.ps1` to create and push the exact tag safely, or use the equivalent
manual commands below. The tag must exactly match `gradle.properties`:

```text
git tag -a v3.0.0-beta.1+mc26.2 -m "U-API 3.0.0-beta.1 for Minecraft 26.2"
git push github v3.0.0-beta.1+mc26.2
```

The prerelease suffix in `mod_version` makes GitHub and Modrinth publish this as a beta. Release
U-API before tagging Soul Ascension because its release build checks out this exact U-API tag.
