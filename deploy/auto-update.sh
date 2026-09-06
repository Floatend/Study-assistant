#!/usr/bin/env bash
set -euo pipefail

# Polls origin/main, then pulls and redeploys when a new commit appears.
# Install as a cron job, for example:
#   */5 * * * * /home/glg/goalbot/Study-assistant/deploy/auto-update.sh

REPO_DIR="${GOALBOT_REPO_DIR:-/home/glg/goalbot/Study-assistant}"
LOG_FILE="${GOALBOT_UPDATE_LOG:-/home/glg/goalbot/auto-update.log}"
LOCK_FILE="${LOG_FILE}.lock"
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S %z')" "$*" >> "$LOG_FILE"
}

if [[ ! -d "$REPO_DIR/.git" ]]; then
  echo "auto-update: repository not found: $REPO_DIR" >&2
  exit 1
fi

cd "$REPO_DIR"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  log "another auto-update is still running"
  exit 0
fi

log "checking origin/main for updates"
if ! git fetch --prune origin main; then
  log "ERROR git fetch failed"
  exit 1
fi

local_head="$(git rev-parse HEAD)"
remote_head="$(git rev-parse origin/main)"
if [[ "$local_head" == "$remote_head" ]]; then
  log "no update (${local_head:0:12})"
  exit 0
fi

if [[ -n "$(git status --porcelain)" ]]; then
  log "ERROR working tree is dirty, refusing to auto-pull"
  exit 1
fi

log "update detected ${local_head:0:12} -> ${remote_head:0:12}"
if ! git pull --ff-only origin main; then
  log "ERROR git pull failed"
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  compose_args=(docker compose)
else
  compose_args=(docker-compose)
fi

log "rebuilding containers"
if ! "${compose_args[@]}" up -d --build; then
  log "ERROR docker compose deploy failed"
  exit 1
fi

log "deploy complete (${remote_head:0:12})"
