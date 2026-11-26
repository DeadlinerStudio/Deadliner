#!/usr/bin/env python3
"""Install one binary from a digest-pinned, versioned Deadliner GHCR package."""
import argparse
import base64
from functools import lru_cache
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile

REGISTRY = "https://ghcr.io"
REPOSITORY = "deadlinerstudio/deadliner-core"
MEDIA = "application/vnd.oci.image.manifest.v1+json"
ASSETS = {
    "android": "deadliner-core-android.aar",
    "ios": "deadliner-core-ios.xcframework.zip",
    "harmony": "libDeadlinerCore.so",
    "macos": "deadliner-core-macos-arm64.framework.zip",
    "cli-macos": "deadliner-macos-arm64.tar.gz",
    "cli-linux": "deadliner-linux-x64.tar.gz",
    "cli-windows": "deadliner-windows-x64.zip",
}


def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return "sha256:" + h.hexdigest()


def github_credentials():
    token = os.environ.get("GHCR_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if token:
        user = os.environ.get("GHCR_USERNAME") or os.environ.get("GITHUB_ACTOR")
        if not user:
            raise ValueError("Set GHCR_USERNAME to the account that owns GHCR_TOKEN.")
        return user, token

    # GUI builds may start with Apple's minimal PATH instead of the user's shell PATH.
    gh = shutil.which("gh")
    if not gh and sys.platform == "darwin":
        gh = next((str(p) for p in (Path("/opt/homebrew/bin/gh"), Path("/usr/local/bin/gh"))
                   if p.is_file()), None)
    if not gh:
        raise ValueError("Install GitHub CLI and log in with read:packages, or set GHCR_USERNAME and GHCR_TOKEN.")
    def query(*args):
        result = subprocess.run([gh, *args], capture_output=True, text=True, check=False)
        if result.returncode or not result.stdout.strip():
            raise ValueError("Log in with gh auth login --hostname github.com --scopes read:packages.")
        return result.stdout.strip()
    user = query("api", "--hostname", "github.com", "user", "--jq", ".login")
    return user, query("auth", "token", "--hostname", "github.com")


@lru_cache(maxsize=1)
def pull_token():
    user, credential = github_credentials()
    exchange = urllib.request.Request(REGISTRY + "/token?service=ghcr.io&scope=repository:" + REPOSITORY + ":pull")
    basic = base64.b64encode((user + ":" + credential).encode("utf-8")).decode("ascii")
    exchange.add_unredirected_header("Authorization", "Basic " + basic)
    try:
        with urllib.request.urlopen(exchange, timeout=45) as response:
            return json.load(response)["token"]
    except urllib.error.HTTPError as exc:
        if exc.code in (401, 403):
            raise ValueError("GHCR denied Core package access. Check package Read permission and "
                             "read:packages (gh auth refresh -h github.com -s read:packages). "
                             "Actions needs packages: read and repository access to the package.") from exc
        raise


def read_registry(kind, name):
    req = urllib.request.Request(f"{REGISTRY}/v2/{REPOSITORY}/{kind}/{name}", headers={"Accept": MEDIA})
    req.add_unredirected_header("Authorization", "Bearer " + pull_token())
    return urllib.request.urlopen(req, timeout=120)


def fetch(kind, checksum, cache):
    if not re.fullmatch(r"sha256:[0-9a-f]{64}", checksum):
        raise ValueError(f"Invalid SHA-256 digest: {checksum}")
    dest = cache / checksum[7:]
    if dest.is_file() and digest(dest) == checksum:
        return dest
    with tempfile.TemporaryDirectory(dir=cache) as temp:
        staged = Path(temp) / "download"
        with read_registry(kind, checksum) as response, staged.open("wb") as out:
            shutil.copyfileobj(response, out)
        if digest(staged) != checksum:
            raise ValueError(f"Digest mismatch downloading {kind}/{checksum}")
        os.replace(staged, dest)
    return dest


def cli_target():
    host, arch = platform.system(), platform.machine().lower()
    if host == "Darwin" and arch in ("arm64", "aarch64"):
        return "cli-macos"
    if host in ("Windows", "Linux") and arch in ("amd64", "x86_64"):
        return "cli-" + host.lower()
    raise ValueError(f"No CLI package for {host} {arch}")


def install_tree(binary, target, destination, checksum, cache):
    entries = {"ios": "Info.plist", "macos": "Shared", "cli-windows": "deadliner.exe",
               "cli-macos": "deadliner", "cli-linux": "bin/deadliner"}
    stamp = cache / ("installed-" + hashlib.sha256(os.fsencode(destination)).hexdigest())
    entry = destination / entries[target]
    def identity():
        st = entry.stat()
        return f"{checksum} {st.st_ino} {st.st_size} {st.st_mtime_ns}"
    if entry.is_file() and stamp.is_file() and stamp.read_text() == identity():
        return
    with tempfile.TemporaryDirectory(dir=destination.parent) as tmp:
        tmp = Path(tmp)
        extracted = tmp / "unpacked"
        extracted.mkdir()
        if target in ("ios", "macos"):
            subprocess.run(["/usr/bin/ditto", "-x", "-k", str(binary), str(extracted)], check=True)
            staged = extracted / ("Shared.xcframework" if target == "ios" else "Shared.framework")
        elif target == "cli-windows":
            with zipfile.ZipFile(binary) as archive:
                archive.extractall(extracted)
            staged = extracted / "deadliner"
        else:
            subprocess.run(["tar", "-xzf", str(binary), "-C", str(extracted)], check=True)
            staged = extracted if target == "cli-macos" else extracted / "deadliner"
            if target == "cli-linux":
                (staged / "deadliner").symlink_to("bin/deadliner")
        if not (staged / entries[target]).is_file():
            raise ValueError(f"Package lacks expected {entries[target]}")
        backup = tmp / "previous"
        if destination.exists():
            os.replace(destination, backup)
        try:
            os.replace(staged, destination)
        except OSError:
            if backup.exists():
                os.replace(backup, destination)
            raise
        stamp.write_text(identity())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", choices=[*ASSETS, "cli"])
    parser.add_argument("destination", type=Path)
    parser.add_argument("--lock", type=Path, required=True, help="JSON with version and manifest digest")
    args = parser.parse_args()
    target = cli_target() if args.target == "cli" else args.target
    lock = json.loads(args.lock.read_text())
    dest = args.destination.absolute()
    dest.parent.mkdir(parents=True, exist_ok=True)
    cache = args.lock.absolute().parent / ".deadliner-core" / "packages"
    cache.mkdir(parents=True, exist_ok=True)
    # Serialize builds sharing one checkout, including Xcode app and extension targets.
    with (cache / "install.lock").open("a+b") as guard:
        if os.name == "nt":
            import msvcrt
            import time
            deadline = time.monotonic() + 300
            while True:
                try:
                    guard.seek(0)
                    msvcrt.locking(guard.fileno(), msvcrt.LK_NBLCK, 1)
                    break
                except OSError:
                    if time.monotonic() >= deadline:
                        raise
                    time.sleep(0.25)
        else:
            import fcntl
            fcntl.flock(guard, fcntl.LOCK_EX)
        manifest = json.loads(fetch("manifests", lock["manifest"], cache).read_bytes())
        if manifest["annotations"]["org.opencontainers.image.version"] != lock["version"]:
            raise ValueError("Version label does not match the pinned manifest")
        layer, = (item for item in manifest["layers"]
                  if item.get("annotations", {}).get("org.opencontainers.image.title") == ASSETS[target])
        checksum = layer["digest"]
        if target in ("android", "harmony") and dest.is_file() and digest(dest) == checksum:
            return
        binary = fetch("blobs", checksum, cache)
        if target in ("android", "harmony"):
            with tempfile.TemporaryDirectory(dir=dest.parent) as tmp:
                staged = Path(tmp) / "binary"
                shutil.copyfile(binary, staged)
                os.replace(staged, dest)
        else:
            install_tree(binary, target, dest, checksum, cache)
    print(f"Installed {ASSETS[target]} {lock['version']} at {dest}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as exc:
        sys.exit(f"Deadliner package installation failed: {exc}")
