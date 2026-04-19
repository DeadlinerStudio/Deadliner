Deadliner core Android sync scripts live here.

`sync_deadliner_core_android.sh` supports:

- `./scripts/sync_deadliner_core_android.sh`
  Downloads the `nightly` Android artifact from GitHub Releases.
- `./scripts/sync_deadliner_core_android.sh nightly`
  Downloads a specific release tag.
- `./scripts/sync_deadliner_core_android.sh local`
  Syncs from the local core repo path.
- `./scripts/sync_deadliner_core_android.sh /abs/path/to/deadliner_core`
  Syncs from a specific local core checkout.

For private GitHub releases, configure a token:

- Environment variable: `DEADLINER_CORE_GITHUB_TOKEN`
- Or Gradle property in `~/.gradle/gradle.properties`:
  `deadliner.core.github.token=ghp_xxx`

Behavior notes:

- Release mode defaults to `nightly` from `DeadlinerStudio/LifiAI-Core`.
- Downloaded artifacts are cached under `.deadliner-core/`.
- If release refresh fails, the script falls back to previously synced cache when available.
- For local mode, set `DEADLINER_CORE_LOCAL_REPO` if your core checkout path is not the default.
