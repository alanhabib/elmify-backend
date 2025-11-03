# Quick Fix: Remove Large Files from Git

⚠️ **BACKUP FIRST!**

## The Problem
Your repository contains 6.17 GiB of data (611 files from `start/` directory with MP3 files). GitHub rejects pushes over ~2GB.

## Quick Solution (Choose One)

### Option 1: git-filter-repo (Recommended)

```bash
# 1. Backup
cd /Users/alanhabib/Desktop/hobby_projects
cp -r elmify-backend elmify-backend-backup

# 2. Install git-filter-repo
brew install git-filter-repo

# 3. Remove start/ directory from all history
cd elmify-backend
git filter-repo --path start/ --invert-paths --force

# 4. Force push
git remote add origin https://github.com/alanhabib/elmify-backend.git
git push origin main --force
```

### Option 2: Simple Approach (If no remote history exists)

```bash
# 1. Remove from tracking
git rm -r --cached start/

# 2. Commit
git commit -m "Remove start/ directory from git tracking"

# 3. Clean history
git filter-branch --index-filter \
  'git rm -rf --cached --ignore-unmatch start/' \
  --prune-empty --tag-name-filter cat -- --all

# 4. Force cleanup
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# 5. Push
git push origin main --force
```

## Verify Success

```bash
# Should show much smaller size
git count-objects -vH

# Should return nothing
git ls-files | grep "^start/"

# Should work now
git push -u origin main
```

## Your Local Files Are Safe
The `start/` directory with your MinIO data will still exist locally - we're only removing it from git history.

---
See `git-push-error-large-repository.md` for detailed explanation.

