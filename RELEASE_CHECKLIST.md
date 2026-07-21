# SOV Admin release checklist

1. Bump verzije u `app/build.gradle.kts`:
   - `versionCode` +1
   - `versionName` s kratkim opisnim sufiksom
2. Build APK lokalno:
   - `./gradlew :app:testDebugUnitTest`
   - `./gradlew :app:compileDebugKotlin`
   - `./gradlew :app:assembleRelease` ili `:app:assembleDebug` za testni APK
3. Preimenuj APK u očekivani release naziv, npr. `SOV-ADMIN-1.4.51a.apk`.
4. Ažuriraj `update.json`:
   - `versionCode`
   - `versionName`
   - `apkFileName`
   - `notes`
   - `releaseDate`
5. Na GitHub release `darkoj-create/SOV-APP-ADMIN` uploadati kao assets:
   - APK
   - `update.json`
6. Tag/release naziv mora pratiti build, npr. `v1.4.51a-maintenance-sync`.
7. U aplikaciji ručno otvoriti Settings → provjera ažuriranja i potvrditi da ne nudi stariji release.

Napomena: ako `update.json` na releaseu slučajno ostane stariji od instalirane aplikacije, aplikacija to tretira kao “nema update” i logira info, ne error.
