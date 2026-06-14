# SOV web v6.1.39i — Real XLS workbook export fix

## Problem
The exported `.xls` file was HTML pretending to be Excel. It contained three HTML tables and Office hints, but it was not a true multi-sheet workbook, so Excel/Google Sheets could open only the first sheet or flatten/ignore tabs.

## Fix
- Replaced fake HTML multi-sheet `.xls` export with SpreadsheetML 2003 XML workbook output.
- Export still downloads as `.xls`, but now contains real `<Worksheet>` nodes:
  1. Aktualna baza
  2. Stara baza
  3. Kombinirano
- Kept current snapshot RPC/export logic from v6.1.39g.
- Kept user catalog hotfix from v6.1.39h.

## Not changed
- No SQL changes.
- No inventory item changes.
- No quantity/location/loan changes.
- No auth changes.

## Cache bust
`assets/oruzar-master-clean.js?v=6.1.39i-real-excel-workbook`
