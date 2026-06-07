SOV Admin APK 1.3.8.2 - Oružarstvo UX flow build fix

Fixes:
- Removed duplicate @Composable annotation before EquipmentCatalogResultHeader.
- Added @Composable to EquipmentPriorityQuickGrid.
- Added @OptIn(ExperimentalLayoutApi::class) to EquipmentPriorityQuickGrid and EquipmentCategoryDeck.
- Kept the 1.3.8.1 UX flow behavior: category click opens item list, search opens results immediately.

Build note:
- Gradle compile could not be executed in this sandbox because the Gradle wrapper cannot download gradle-9.0.0-bin.zip without internet.
- Build in Android Studio as usual and sign with the existing admin keystore.
