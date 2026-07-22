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
git commit -m "Fix U-API HUD registration on Minecraft 26.2"
git push -u github port/26.2
```

Wait for the branch `CI` workflow, then perform a local release dry-run from the synchronized
version branch. Merging into `master` is optional and is not required by the release workflow:

```text
.\scripts\release.ps1 -DryRun
```

Then run `.\scripts\release.ps1` to create and push the exact tag safely, or use the equivalent
manual commands below. The tag must exactly match `gradle.properties`:

```text
git tag -a v3.0.0-beta.2+mc26.2 -m "U-API 3.0.0-beta.2 for Minecraft 26.2"
git push github v3.0.0-beta.2+mc26.2
```

The prerelease suffix in `mod_version` makes GitHub and Modrinth publish this as a beta. Release
U-API before tagging Soul Ascension because its release build checks out this exact U-API tag.
