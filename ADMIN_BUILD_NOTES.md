# SOV Admin v1.1.1-admin.1

Private/admin branch intended to live in parallel with the public release.

## Changed
- Package/applicationId: `com.darko.speleov1admin`, so it can be installed next to public SOV.
- App name: `SOV Admin`.
- Version: `versionCode 900001`, `versionName 1.1.1-admin.1`.
- Update track: AppUpdateManager points to GitHub repo `darkoj-create/SOV-APP-ADMIN` and update.json uses admin APK name.
- Katastar-only records are restored into the loaded search dataset.
- Search source filter has explicit `Katastar` button and `Sve = SOV + Katastar + Moja`.
- Shared trips now show admin edit/share/delete controls even when they are not local packages.
- Shared trips refresh shows a short admin toast with the number of loaded Sheet trips.

## Notes
- Existing Apps Script endpoint stays the same: `AKfycbybGi7p6_ImXAXEErJ6P9K0GYHy8lHW850K9cQe2py8yUV2oJO6UW1DJi00quorVTHOGQ`.
- Admin edit of a shared trip works by deleting the old Sheet row and adding the edited row back through the existing endpoint.
- I could not compile APK in this sandbox because Gradle wrapper tried to download Gradle 9.0.0 and the environment has no internet access.
