# Deployment

## Goal

Publish the catalog in one of two ways:

- GitHub Pages for internet or GitHub-internal browsing
- Internal static hosting for intranet environments

Both options use the same generated static site.

## Deploy Artifact

The deployable artifact is exported to:

- [dist/site](../dist/site)

Generate it with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\export-site.ps1
```

This script:

1. Regenerates catalog data from `skills/*/meta.json`
2. Copies the catalog into `dist/site`
3. Adds `.nojekyll` for GitHub Pages compatibility

`dist/` is a generated export directory and should not be committed. Recreate it with the export script whenever you need a fresh deploy artifact.

## Option 1: GitHub Pages

This repository includes a workflow at [deploy-pages.yml](../.github/workflows/deploy-pages.yml).

**Important:** Enable Pages **before** the workflow can succeed (see troubleshooting below if you see `Get Pages site failed` / `Not Found`).

Recommended GitHub repository settings:

1. Push this repository to GitHub.
2. In repository settings, open **Settings → Pages**.
3. Under **Build and deployment**, set **Source** to **GitHub Actions** (not “Deploy from a branch”).
4. Merge changes to `main` (or run the workflow manually from the **Actions** tab).

### Troubleshooting: `configure-pages` / `Get Pages site failed` / `Not Found`

If **Annotations** show:

`Get Pages site failed. Please verify that the repository has Pages enabled and configured to build using GitHub Actions`

then Pages is not enabled for this repo, or the source is still “Deploy from a branch”.

**Fix:**

1. Go to the repository on GitHub → **Settings** → **Pages**.
2. **Build and deployment** → **Source** → choose **GitHub Actions**.
3. Save if prompted, then **re-run failed jobs** (Actions → failed run → **Re-run all jobs**).

Optional: **Settings → Actions → General** → **Workflow permissions** → enable **Read and write permissions** if your org allows it (this repo’s workflow uses `pages: write` and `id-token: write`; default `GITHUB_TOKEN` is usually enough after Pages is enabled).

The `enablement: true` input on `actions/configure-pages` only works with a **personal access token** (or app) with extra scopes—not the default `GITHUB_TOKEN`—so manual **Source: GitHub Actions** is the usual fix.

### 中文摘要（同上）

若 Actions 里出现 **Not Found** / **Get Pages site failed**：说明还没在仓库里打开 Pages，或没有用 **GitHub Actions** 作为发布源。

1. 打开 **设置 → Pages**。  
2. **构建与部署** 里把 **源** 选成 **GitHub Actions**（不要选「从分支部署」）。  
3. 回到 **Actions**，**重新运行** 失败的工作流。

完成后再推送 `main` 或手动触发 **Deploy Catalog To GitHub Pages** 即可。

The workflow will:

1. Check out the repository
2. Export `dist/site`
3. Upload the static artifact
4. Deploy to GitHub Pages

Typical published URL:

- `https://<org>.github.io/<repo>/`

If you later want a custom domain, add a `CNAME` file during export or in a follow-up workflow step.

## Option 2: Internal Static Hosting

For Nginx, Apache, IIS, or an internal object-storage site, publish the contents of [dist/site](../dist/site).

Typical workflow:

1. Run `powershell -ExecutionPolicy Bypass -File .\scripts\export-site.ps1`
2. Copy the files in `dist/site` to your static host root
3. Point the internal URL to that folder

Examples:

- IIS site physical path -> `dist/site`
- Nginx root -> copied `dist/site` contents
- Internal artifact pipeline -> zip and upload `dist/site`

## Recommended Choice

- Use GitHub Pages if your team already works in GitHub and the catalog can be visible there
- Use an internal static site if the catalog or skill metadata should stay inside the company network

## Operational Notes

- Re-run export after any `meta.json` change
- Commit generated catalog data so local preview stays fast
- Do not commit generated `dist/site`; the workflow or local export step rebuilds it as needed
- For internal hosting, either publish from CI or publish manually from the exported folder
