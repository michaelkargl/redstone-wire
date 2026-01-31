---
id: RW-001
title: Introduce GitHub Actions CI/CD
status: To Do
assignee: []
created_date: '2026-01-28 10:17'
updated_date: '2026-01-31 09:52'
labels: []
milestone: m-0
dependencies: []
references:
  - /.github/workflows/build.yml
  - /run_tests.sh
  - /build.gradle
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add GitHub Actions CI/CD workflow that builds and runs the tests on every push and pull request. Merging is only possible if both are successful (when branch protection is enabled) and artifact is downloadable
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 PR merges are only accepted if game tests are successfully run (run_tests.sh)
- [ ] #2 Mod is built and artifact is downloadable
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation Approach

### Workflow Structure
- **2-job sequential workflow**: Build → Test
- **Runner**: `ubuntu-latest` with JDK 21 (Temurin distribution)
- **Build Job**: `./gradlew build` + artifact publishing
- **Test Job**: `./run_tests.sh` (NeoForge GameTest server)

### Key Technical Decisions

**Caching Strategy**:
- Gradle caching automatically managed by `gradle/actions/setup-gradle@v4`
- No manual cache configuration needed
- Gradle wrapper and dependencies cached between runs

**Artifact Management**:
- Publish: `build/libs/minecraftplayground-1.0.0.jar`
- Retention: 30 days for build artifacts, 7 days for test logs
- Naming: Include branch + commit SHA
- Upload on success (build) and always (test logs)

**Branch Protection (Optional)**:
- GitHub UI: Settings → Branches → Add protection rule
- Require status checks: Enable `build` and `test` jobs
- Blocks merge button on workflow failure

### Files Modified
1. `.github/workflows/build.yml` - Update existing workflow to add test job and artifacts

### Files to Configure (GitHub UI - Optional)
1. Settings → Branches → Add branch protection rule
2. Enable "Require status checks to pass before merging"
3. Select `build` and `test` as required checks

### Verification Steps
1. Push changes to trigger workflow
2. Verify both build and test jobs complete
3. Check artifact downloadability from Actions tab
4. Test failure scenario (intentional test failure)
5. Optionally configure branch protection and confirm merge blocking works

### Advantages over GitLab CI
- **GitHub Actions** is already configured (existing workflow)
- Native integration with GitHub pull requests
- Gradle caching built-in via official action
- Free for public repositories (2,000 minutes/month for private)
- No separate platform required
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Current Status: Implementation Complete

### Changes Made

**GitHub Actions Workflow** (`.github/workflows/build.yml`):
- Updated existing workflow to add dedicated test job
- Build job: Compiles mod and uploads JAR artifacts (30-day retention)
- Test job: Runs `run_tests.sh` and uploads logs (7-day retention)
- Both jobs use JDK 21 (Temurin) on ubuntu-latest
- Gradle caching handled automatically by setup-gradle action

**Documentation** (`README.md`):
- Added CI/CD Status section with GitHub Actions badge
- Documented workflow overview and job stages
- Provided artifact download instructions
- Added optional branch protection setup guide
- Included troubleshooting for common issues

### Testing Needed
1. Push to trigger workflow and verify both jobs pass
2. Download artifacts from Actions tab
3. Optionally enable branch protection for merge blocking

### Reference
- Updated workflow: `/.github/workflows/build.yml`
- Documentation: `/README.md`

### Next Steps for Completion

**Step 1: Update the README Badge**
Replace the placeholder in `README.md` line 6 with actual GitHub repository details:
```markdown
[![Build and Test](https://github.com/YOUR-USERNAME/MinecraftPlayground/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR-USERNAME/MinecraftPlayground/actions/workflows/build.yml)
```

**Step 2: Test the Workflow**
Push changes to trigger the workflow:
```bash
git add .github/workflows/build.yml README.md backlog/
git commit -m "Add GitHub Actions test job and artifacts"
git push
```

Verify:
1. Go to **Actions** tab in GitHub
2. Check that both `build` and `test` jobs complete successfully
3. Download artifacts from the workflow run
4. Verify the JAR file is included in the artifact

**Step 3: Enable Branch Protection (Optional - Required for Acceptance Criteria #1)**
To block merges when tests fail:
1. Go to **Settings → Branches** in GitHub
2. Click **Add rule** (or **Add branch protection rule**)
3. Enter `main` (or default branch name) in **Branch name pattern**
4. ☑️ Enable **"Require status checks to pass before merging"**
5. Search for and select `build` and `test` as required checks
6. ☑️ Enable **"Require branches to be up to date before merging"** (recommended)
7. Click **Create** (or **Save changes**)

**Completion Checklist**:
- [ ] README badge updated with actual repository URL
- [ ] Workflow tested and both jobs pass
- [ ] Artifacts downloadable from Actions tab
- [ ] Branch protection enabled (if requiring merge blocking)
<!-- SECTION:NOTES:END -->
