#!/usr/bin/env bash

set -euo pipefail

ANDROID_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_CORE_REPO="/Users/aritxonly/Codes/Agent/deadliner_core"
DEFAULT_RELEASE_REPO="DeadlinerStudio/LifiAI-Core"
DEFAULT_RELEASE_TAG="nightly"
ARTIFACT_NAME="deadliner-android.zip"
SYNC_ROOT="${ANDROID_REPO_ROOT}/.deadliner-core/android"
CACHE_ROOT="${ANDROID_REPO_ROOT}/.deadliner-core/cache"
STATE_FILE="${ANDROID_REPO_ROOT}/.deadliner-core/android-sync-state.json"

MODE="${1:-${DEADLINER_CORE_SOURCE:-release}}"
RELEASE_TAG="${DEADLINER_CORE_TAG:-$DEFAULT_RELEASE_TAG}"
RELEASE_REPO="${DEADLINER_CORE_RELEASE_REPO:-$DEFAULT_RELEASE_REPO}"
LOCAL_CORE_REPO="${DEADLINER_CORE_LOCAL_REPO:-$DEFAULT_CORE_REPO}"
GITHUB_API_ROOT="${DEADLINER_CORE_GITHUB_API_ROOT:-https://api.github.com}"

DEST_JNI_LIBS="${SYNC_ROOT}/jniLibs"
DEST_BINDINGS="${SYNC_ROOT}/bindings"

resolve_github_token() {
  if [[ -n "${DEADLINER_CORE_GITHUB_TOKEN:-}" ]]; then
    printf '%s' "${DEADLINER_CORE_GITHUB_TOKEN}"
    return 0
  fi

  if [[ -n "${GITHUB_TOKEN:-}" ]]; then
    printf '%s' "${GITHUB_TOKEN}"
    return 0
  fi

  if command -v gh >/dev/null 2>&1; then
    gh auth token 2>/dev/null || true
    return 0
  fi

  return 0
}

AUTH_TOKEN=""

require_path() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "error: missing required path: $path" >&2
    exit 1
  fi
}

ensure_tools() {
  command -v curl >/dev/null 2>&1 || {
    echo "error: curl is required" >&2
    exit 1
  }
  command -v python3 >/dev/null 2>&1 || {
    echo "error: python3 is required" >&2
    exit 1
  }
}

auth_hint() {
  cat >&2 <<'EOF'
hint: this repo is private, so release sync needs GitHub auth.
hint: set DEADLINER_CORE_GITHUB_TOKEN (recommended) or GITHUB_TOKEN before building.
hint: example:
hint:   export DEADLINER_CORE_GITHUB_TOKEN=ghp_xxx
EOF
}

has_cached_sync() {
  [[ -d "${DEST_JNI_LIBS}" && -d "${DEST_BINDINGS}" ]] || return 1
  [[ -n "$(find "${DEST_JNI_LIBS}" -mindepth 1 -print -quit 2>/dev/null)" ]] || return 1
  [[ -n "$(find "${DEST_BINDINGS}" -mindepth 1 -print -quit 2>/dev/null)" ]] || return 1
}

fallback_to_cached_sync() {
  local reason="$1"
  if has_cached_sync; then
    echo "warning: ${reason}" >&2
    echo "warning: failed to refresh release artifact, falling back to cached Android core files" >&2
    return 0
  fi

  echo "error: ${reason}" >&2
  echo "error: no usable cached Android core files found in ${SYNC_ROOT}" >&2
  return 1
}

clean_destinations() {
  rm -rf "${DEST_JNI_LIBS}" "${DEST_BINDINGS}"
  mkdir -p "${DEST_JNI_LIBS}" "${DEST_BINDINGS}"
}

sync_from_local_repo() {
  local core_repo="$1"
  local src_jni src_bindings

  src_jni="${core_repo}/dist/android/jniLibs"
  src_bindings="${core_repo}/dist/android/bindings"

  echo "==> Syncing Android artifacts from local core repo"
  echo "    core repo: ${core_repo}"

  require_path "${src_jni}"
  require_path "${src_bindings}"

  clean_destinations
  cp -R "${src_jni}/." "${DEST_JNI_LIBS}/"
  cp -R "${src_bindings}/." "${DEST_BINDINGS}/"

  mkdir -p "$(dirname "${STATE_FILE}")"
  python3 - <<'PY' "${STATE_FILE}" "${core_repo}"
from pathlib import Path
import json
import sys

state_path = Path(sys.argv[1])
core_repo = sys.argv[2]
state_path.write_text(json.dumps({
    "mode": "local",
    "source": core_repo,
}, indent=2) + "\n")
PY
}

fetch_release_metadata() {
  local response_file="$1"
  local -a curl_args

  curl_args=(
    -fsSL
    -H "Accept: application/vnd.github+json"
    -H "X-GitHub-Api-Version: 2022-11-28"
  )

  if [[ -n "${AUTH_TOKEN}" ]]; then
    curl_args+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
  fi

  curl_args+=(
    "${GITHUB_API_ROOT}/repos/${RELEASE_REPO}/releases/tags/${RELEASE_TAG}"
    -o "${response_file}"
  )

  curl "${curl_args[@]}"
}

download_release_asset() {
  local metadata_file="$1"
  local output_file="$2"
  local asset_api_url

  if ! asset_api_url="$(python3 - <<'PY' "${metadata_file}" "${ARTIFACT_NAME}"
from pathlib import Path
import json
import sys

metadata_path = Path(sys.argv[1])
asset_name = sys.argv[2]

metadata = json.loads(metadata_path.read_text())
assets = metadata.get("assets", [])
asset_url = None
for asset in assets:
    if asset.get("name") == asset_name:
        asset_url = asset.get("url")
        break

if not asset_url:
    raise SystemExit(f"error: asset {asset_name} not found in release metadata")

print(asset_url)
PY
)"; then
    return 1
  fi

  local -a curl_args
  curl_args=(
    -fsSL
    -H "Accept: application/octet-stream"
    -H "X-GitHub-Api-Version: 2022-11-28"
  )

  if [[ -n "${AUTH_TOKEN}" ]]; then
    curl_args+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
  fi

  curl_args+=("${asset_api_url}" -o "${output_file}")
  curl "${curl_args[@]}"
}

extract_release_artifact() {
  local archive_file="$1"
  local extract_dir="$2"

  python3 - <<'PY' "${archive_file}" "${extract_dir}"
from pathlib import Path
import shutil
import sys
import zipfile

archive = Path(sys.argv[1])
extract_dir = Path(sys.argv[2])

if extract_dir.exists():
    shutil.rmtree(extract_dir)
extract_dir.mkdir(parents=True, exist_ok=True)

with zipfile.ZipFile(archive) as zf:
    zf.extractall(extract_dir)
PY
}

sync_from_release() {
  local metadata_file zip_file extract_dir commit_sha cached_sha remote_sha

  mkdir -p "${CACHE_ROOT}" "$(dirname "${STATE_FILE}")"
  metadata_file="${CACHE_ROOT}/release.json"
  zip_file="${CACHE_ROOT}/${ARTIFACT_NAME}"
  extract_dir="${CACHE_ROOT}/unzipped"

  echo "==> Syncing Android artifacts from GitHub release"
  echo "    repo: ${RELEASE_REPO}"
  echo "    tag: ${RELEASE_TAG}"

  if [[ -z "${AUTH_TOKEN}" ]]; then
    echo "warning: no GitHub token detected; private release access may fail." >&2
  fi

  if ! fetch_release_metadata "${metadata_file}"; then
    auth_hint
    fallback_to_cached_sync "failed to fetch release metadata for ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi

  if ! remote_sha="$(python3 - <<'PY' "${metadata_file}"
from pathlib import Path
import json
import re
import sys

metadata = json.loads(Path(sys.argv[1]).read_text())
body = metadata.get("body") or ""
match = re.search(r"Commit:\s*([0-9a-fA-F]{7,40})", body)
print(match.group(1) if match else metadata.get("target_commitish", "unknown"))
PY
)"; then
    fallback_to_cached_sync "failed to parse release metadata for ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi

  cached_sha="$(python3 - <<'PY' "${STATE_FILE}"
from pathlib import Path
import json
import sys

state_path = Path(sys.argv[1])
if not state_path.exists():
    print("")
else:
    data = json.loads(state_path.read_text())
    print(data.get("commit", ""))
PY
)"

  if [[ -n "${cached_sha}" && "${cached_sha}" == "${remote_sha}" && -d "${DEST_JNI_LIBS}" && -d "${DEST_BINDINGS}" ]]; then
    echo "==> Android core artifacts already up to date at ${remote_sha}"
    return 0
  fi

  if ! download_release_asset "${metadata_file}" "${zip_file}"; then
    auth_hint
    fallback_to_cached_sync "failed to download ${ARTIFACT_NAME} from ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi
  if ! extract_release_artifact "${zip_file}" "${extract_dir}"; then
    fallback_to_cached_sync "failed to extract ${zip_file}"
    return $?
  fi

  require_path "${extract_dir}/android/jniLibs"
  require_path "${extract_dir}/android/bindings"

  clean_destinations
  cp -R "${extract_dir}/android/jniLibs/." "${DEST_JNI_LIBS}/"
  cp -R "${extract_dir}/android/bindings/." "${DEST_BINDINGS}/"

  python3 - <<'PY' "${STATE_FILE}" "${RELEASE_REPO}" "${RELEASE_TAG}" "${remote_sha}"
from pathlib import Path
import json
import sys

state_path = Path(sys.argv[1])
repo = sys.argv[2]
tag = sys.argv[3]
commit = sys.argv[4]

state_path.write_text(json.dumps({
    "mode": "release",
    "repo": repo,
    "tag": tag,
    "commit": commit,
}, indent=2) + "\n")
PY

  echo "==> Synced Android core artifacts at commit ${remote_sha}"
}

main() {
  ensure_tools
  AUTH_TOKEN="$(resolve_github_token)"

  mkdir -p "${SYNC_ROOT}"

  case "${MODE}" in
    local)
      sync_from_local_repo "${LOCAL_CORE_REPO}"
      ;;
    release)
      sync_from_release
      ;;
    /*)
      sync_from_local_repo "${MODE}"
      ;;
    *)
      RELEASE_TAG="${MODE}"
      sync_from_release
      ;;
  esac

  echo "==> Android JNI libs: ${DEST_JNI_LIBS}"
  echo "==> Android Kotlin bindings: ${DEST_BINDINGS}"
}

main "$@"
