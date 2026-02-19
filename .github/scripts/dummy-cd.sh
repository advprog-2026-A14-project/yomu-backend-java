#!/usr/bin/env bash
set -euo pipefail

TARGET_ENV="${1:-}"

if [[ -z "${TARGET_ENV}" ]]; then
  echo "Usage: $0 <staging|production>"
  exit 1
fi

echo "Starting dummy deployment..."
echo "Target environment: ${TARGET_ENV}"
echo "Commit SHA: ${GITHUB_SHA:-unknown}"
echo "Branch ref: ${GITHUB_REF:-unknown}"
echo "Dummy deployment completed successfully."
