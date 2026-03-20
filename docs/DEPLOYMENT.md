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

## Option 1: GitHub Pages

This repository includes a workflow at [deploy-pages.yml](../.github/workflows/deploy-pages.yml).

Recommended GitHub repository settings:

1. Push this repository to GitHub.
2. In repository settings, open `Pages`.
3. Set `Source` to `GitHub Actions`.
4. Merge changes to `main`.

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
- For GitHub Pages, generated `dist/site` does not need to be committed because the workflow rebuilds it
- For internal hosting, either publish from CI or publish manually from the exported folder
