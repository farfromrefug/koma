Looking to report an issue/bug or make a feature request? Please refer to the [README file](https://github.com/farfromrefug/koma#issues-feature-requests-and-contributing).

---

Thanks for your interest in contributing to Koma!


# Code contributions

Pull requests are welcome!

If you're interested in taking on [an open issue](https://github.com/farfromrefug/koma/issues), please comment on it so others are aware.
You do not need to ask for permission nor an assignment.

## Prerequisites

Before you start, please note that the ability to use following technologies is **required** and that existing contributors will not actively teach them to you.

- Basic [Android development](https://developer.android.com/)
- [Kotlin](https://kotlinlang.org/)

### Tools

- [Android Studio](https://developer.android.com/studio)
- Emulator or phone with developer options enabled to test changes.

<!-- ## Getting help -->

<!-- - Join [the Discord server](https://discord.gg/mihon) for online help and to ask questions while developing. -->

# Releases

## Creating a release

There are two ways to create a release:

### 1. Tag-based release

Push a tag to the repository:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This will trigger the release workflow which will:
- Update `versionName` in `app/build.gradle.kts` to match the tag (without the `v` prefix)
- Increment `versionCode` by 1
- Commit these changes to the branch
- Build and sign the APKs
- Generate a changelog using git-cliff
- Create a GitHub release

### 2. Manual release

Go to the [Actions tab](https://github.com/farfromrefug/koma/actions/workflows/release.yml) and click "Run workflow". Select the version bump type:

- **patch**: Increments the patch version (e.g., 1.0.0 → 1.0.1)
- **minor**: Increments the minor version and resets patch to 0 (e.g., 1.0.0 → 1.1.0)
- **major**: Increments the major version and resets minor and patch to 0 (e.g., 1.0.0 → 2.0.0)

This will:
- Bump the version according to your selection
- Increment `versionCode` by 1
- Commit these changes to the current branch
- Create and push the corresponding tag (e.g., `v1.0.1`)
- Build and sign the APKs
- Generate a changelog using git-cliff
- Create a GitHub release

# Translations

Translations are done externally via [Weblate](https://hosted.weblate.org/projects/koma/koma/).


# Forks

Forks are allowed so long as they abide by [the project's LICENSE](https://github.com/farfromrefug/koma/blob/main/LICENSE).

When creating a fork, remember to:

- To avoid confusion with the main app:
    - Change the app name
    - Change the app icon
    - Change or disable the [app update checker](https://github.com/farfromrefug/koma/blob/main/app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateChecker.kt)
- To avoid installation conflicts:
    - Change the `applicationId` in [`build.gradle.kts`](https://github.com/farfromrefug/koma/blob/main/app/build.gradle.kts)
- To avoid having your data polluting the main app's analytics and crash report services:
    - If you want to use Firebase analytics, replace [`google-services.json`](https://github.com/farfromrefug/koma/blob/main/app/src/standard/google-services.json) with your own
