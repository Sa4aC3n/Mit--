# Category Seed Data — Development Tools

These files are **development-only tools** used to define and regenerate the initial
business-category seed data for the Dalil Mit Ghamr app.

## Files

| File | Description |
|------|-------------|
| `dalil_mit_ghamr_categories.xls` | Legacy Excel 2003 seed data (59 KB) |
| `dalil_mit_ghamr_categories.xlsx` | Modern Excel seed data (10 KB) |

## Important Notes

- **These files are NOT bundled in the Android APK** and are NOT read at runtime.
- The app reads Excel files only from **user-selected URIs** via Android's
  `contentResolver.openInputStream(uri)` (see `ExcelFileImportHelper.kt`).
- These files were previously duplicated under `app/src/main/assets/` (now removed).
- The Python scripts at the repo root (`generate_categories_excel.py`,
  `create_modern_xlsx.py`) were used to generate these files.

## How to Regenerate Seed Data

1. Edit the Python scripts at the repo root as needed.
2. Run the script to regenerate the Excel files here.
3. Use the Admin panel in the app to import the updated Excel file manually.

## Why Here?

Moved from `app/src/main/assets/` and the repo root to this `tools/categories/`
folder during Stage A-B cleanup (commit: `cleanup(stage-a-b)`) to:

- Keep dev tooling separate from Android runtime assets.
- Prevent ~70 KB of unused seed files from being packaged into every APK.
- Provide a clear home for future category data maintenance.
