# Troubleshooting Index

This directory contains troubleshooting guides and solutions for common issues in the elmify-backend project.

## Available Guides

### 1. [Git Push Error: Large Repository](./git-push-error-large-repository.md)
**Issue:** HTTP 500 error when pushing to GitHub  
**Cause:** Repository contains 6.17 GiB of data from `start/` directory (MinIO storage with audio files)  
**Solution:** Remove large files from git history using `git-filter-repo`

**Quick Links:**
- [Detailed Guide](./git-push-error-large-repository.md) - Complete analysis and step-by-step solution
- [Quick Fix Guide](./QUICK_FIX_LARGE_REPO.md) - Fast commands to resolve the issue

### Prevention Measures Implemented

1. **Pre-commit Hook:** Automatically checks for files >50MB before commit
   - Location: `.git/hooks/pre-commit`
   - Blocks commits with files >50MB
   - Warns about files >10MB

2. **Updated .gitignore:** Ensures `start/` directory is excluded
   ```gitignore
   # MinIO data (development only - exclude from git)
   start/
   ```

## Common Issues

### Issue: "Everything up-to-date" but push fails
This occurs when local commits are present but git history contains large files that can't be pushed.

**Solution:** Clean git history by removing large files (see guides above)

### Issue: Files in .gitignore still tracked
Adding files to `.gitignore` doesn't remove them if already tracked.

**Solution:** 
```bash
git rm -r --cached start/
git commit -m "Remove tracked files now in .gitignore"
```

### Issue: Repository size too large
GitHub recommends repositories under 1GB.

**Solution:** Use external storage for binary/media files:
- Local development: MinIO (`start/` directory)
- Production: AWS S3
- Never commit media files to git

## Quick Diagnostics

### Check repository size
```bash
git count-objects -vH
```

### Find large files
```bash
git ls-files | xargs -I {} ls -lh {} 2>/dev/null | \
  awk '{if ($5 ~ /M/) print $5, $9}' | sort -hr | head -20
```

### Check tracked files in start/
```bash
git ls-files | grep "^start/"
```

## Getting Help

If you encounter issues not covered here:

1. Check the error message carefully
2. Search GitHub documentation
3. Review application logs in `logs/` directory
4. Check Spring Boot configuration in `src/main/resources/`

## Related Documentation

- `docs/learning/` - Learning resources for technologies used
- `MIGRATION.md` - Database migration guide
- `PRODUCTION_DEPLOYMENT.md` - Production deployment guide

---

**Last Updated:** November 3, 2025

