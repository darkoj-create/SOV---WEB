# First full audit artifact baseline

The first complete repository/browser artifact was generated before the legacy-path normalization batch.

- HTML pages scanned: **280**
- Static release blockers: **202**
- Static warnings: **27**
- Browser errors: **515**
- Browser warnings: **220**

The counts were dominated by four repeated root causes rather than hundreds of independent defects:

1. 187 nested news pages requested `/novosti/update.json` because the shared version helper used a document-relative URL.
2. Imported news pages requested `/novosti/assets/...` instead of root assets.
3. `/login/`, `/prijava/` and `/clanska-zona/` contained duplicated pages with root-relative assumptions expressed as nested relative URLs.
4. Legacy alias pages mixed Vercel routing, JavaScript redirects and meta refreshes.

The audit branch contains deterministic normalization and repeatable checks. This baseline is retained so later artifacts can prove the blocker count decreased instead of relying on visual impressions.
