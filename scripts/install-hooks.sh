#!/bin/bash
# Installs this repo's git hooks (currently: gitleaks pre-commit secret scanning)
# Run once after cloning: bash scripts/install-hooks.sh

REPO_ROOT="$(git rev-parse --show-toplevel)"

cp "$REPO_ROOT/scripts/pre-commit" "$REPO_ROOT/.git/hooks/pre-commit"
chmod +x "$REPO_ROOT/.git/hooks/pre-commit"

echo "✅ Pre-commit hook installed. Requires gitleaks: https://github.com/gitleaks/gitleaks"
echo "   (Ubuntu/Debian: sudo apt install gitleaks)"
