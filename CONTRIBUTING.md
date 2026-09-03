# Contributing

## Release

Releases are published to Maven Central by [`.github/workflows/maven.yaml`](./.github/workflows/maven.yaml), which triggers on any tag push. The tag name itself becomes the published version (see `sdk/build.gradle.kts`), so it must be an exact semantic version with no `v` prefix (e.g. `4.0.0`).

To cut a release:

1. **Draft the changelog** - Add a new entry to [`CHANGELOG.md`](./CHANGELOG.md) summarizing what changed since the last release. Decide the version number based on [Semantic Versioning](https://semver.org/): bump major for breaking API changes, minor for backward-compatible additions, patch for fixes only.
2. **Bump the version** - Update the version number used in the changelog entry and anywhere else it's referenced (e.g. sample app docs). The SDK artifact version itself is not stored in a file - it comes from the git tag pushed in the next step.
3. **Open a PR** - Push a branch with the changelog/version changes and open a PR against `main`. Get it reviewed and merged.
4. **Push tag** - Once merged, tag the resulting commit on `main` with the version number and push the tag to the `authgear/authgear-sdk-android` repository:

   ```sh
   git fetch authgear
   git tag 4.0.0 authgear/main
   git push authgear 4.0.0
   ```

   Pushing the tag triggers the workflow to run the test suite and, if it passes, publish the new version to Maven Central.
