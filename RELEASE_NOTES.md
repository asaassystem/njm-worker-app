# NJM Worker App - Release Notes

## v6.0 (versionCode 72) - 2026-03-18

### Overview
Complete improvement, redesign, and compatibility alignment release for the NJM Worker Android application.

### What Changed

#### Kotlin Source Files

**TodayFragment.kt**
- Fixed double-load bug (removed onResume duplicate load)
- Worker name now appears on all printed receipts
- Loading ProgressBar state management added
- Error Toast feedback on network failure

**MonthFragment.kt**
- Worker name included on all printed receipts
- Error feedback on network failure
- Loading state visibility management

**InvoiceAdapter.kt** (NEW)
- New RecyclerView.Adapter for invoice list
- Status badge color coding: paid=green, issued=blue, draft=gray
- Displays: invoice number, status, period, wash count, total, VAT, date
- Print button per invoice row with callback

**InvoicesFragment.kt**
- Replaced ListView + ArrayAdapter with RecyclerView + InvoiceAdapter
- Loading ProgressBar state management
- Error feedback on load failure
- Kept date picker and invoice creation flow intact

**PrintManager.kt**
- Worker name now fetched from SessionManager for ALL print calls
- Previously workerName was always passed as empty string
- Affects: printWashReceipt (both overloads), printReceiptForCar, printTest

**NjmApp.kt**
- Fixed tryBindSunmiPrinter to call real PrinterManager.init()
- Previously used non-functional stub implementation

#### Layout XML Files

**fragment_today.xml**
- Added tvTodayCount (wash count stat column)
- 4-column stats card: count, total, paid, unpaid
- Improved spacing and typography (11sp labels)

**fragment_invoices.xml**
- Replaced ListView with RecyclerView (id: rvInvoices)
- Added NJM header section with logo
- Improved layout structure

**item_invoice.xml** (NEW)
- CardView layout for invoice list items
- Fields: number, status badge, period, washes count, total, VAT, print button, date

#### Build

**app/build.gradle**
- versionCode: 71 -> 72
- versionName: "5.0" -> "6.0"

---

## Rollback to v5.0

To roll back to the previous version:

1. Find the last v5.0 commit before the v6.0 changes:
   - Look in git history for commit before "v6.0: Bump versionCode 71->72"
   - The previous stable SHA was before the v6.0 commits series

2. Revert files changed:
   - app/build.gradle (versionCode 71, versionName "5.0")
   - TodayFragment.kt (revert from git history)
   - MonthFragment.kt (revert from git history)
   - NjmApp.kt (revert from git history)
   - InvoicesFragment.kt (revert from git history)
   - PrintManager.kt (revert from git history)
   - fragment_today.xml (revert from git history)
   - fragment_invoices.xml (revert from git history)

3. Files that are NEW in v6.0 (did not exist in v5.0):
   - InvoiceAdapter.kt
   - item_invoice.xml
   - RELEASE_NOTES.md

4. The v5.0 APK (versionCode 71) remains accessible in GitHub Actions artifacts.

---

## Build Instructions

```
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

The CI/CD pipeline (.github/workflows/android.yml) builds automatically on push to main.

---

*NJM Worker App - Internal operational tool for NJM Car Wash (mghslt njm)*
*Developer: meshari.tech*
