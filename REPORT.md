# Ruwia — v2.0 Release Report

**Version:** 2.0 (versionCode 2) · **Flavor:** `prod` · **Status:** Released
**Branch:** `feature/phase1/final` · **Tag:** `v2.0`

This report documents every change shipped in v2.0, grouped by category, with the
underlying cause, the fix, and how to verify each item.

---

## 1. Bug Fixes

### 1.1 Empty-Cases "Reset" did not persist (admin + employee Stock tab)

**Symptom.** The *Empty Cases* figure kept coming back after tapping Reset, on the
admin metrics card, the per-shop stock chips, and the employee Stock tab.

**Root cause (two parts).**
1. The reset only persisted a baseline row in `app_settings`, but that table was
   added to `supabase_schema.sql` after the live database was created, and tenant
   isolation (RLS) blocked anonymous writes. `getEmptyCansBaseline()` returned `-1`
   and `resetEmptyCases()` failed silently, so the app kept deriving the live total
   from raw movement sums.
2. Even when the baseline persisted, the **per-shop** chips computed raw movement
   sums and never subtracted the baseline, so each shop still showed the old number
   while the headline card showed 0.

**Fix.**
- `getEmptyCansBaseline()` and `resetEmptyCases()` now retry through the
  service-role client (`supabaseAdmin`) when the anonymous/RLS path fails, and a
  clear error is surfaced to the user when both paths fail (instead of silent
  no-ops).
- `deriveShopStockTotals()` now takes an `emptyCansBaseline` parameter and subtracts
  it per shop (`EmployeeStockScreen`, `StockDashboardScreen`, `EmployeeDashboardScreen`).
- Inconsistent empty-cans source filters (`"Empty cans"`-only) were replaced with
  `isEmptyCansSource()` everywhere.

**DB requirement.** `app_settings` + `migrate_to_tenant_table('app_settings')` from
`supabase_schema.sql` must be run in the Supabase SQL Editor for baseline persistence.

**Verify.** Reset Empty Cases on the admin dashboard → the headline card and both
per-shop chips show 0 and stay 0 after a restart/refresh.

### 1.2 Movement history was truncated

**Symptom.** Stock-ledger counts (KPIs, Stock History, live figures) showed a small
fraction of the real totals once history grew.

**Root cause.** Movement feeds were capped (e.g. `limit(5000)`), so older movements
silently dropped and every derived count corrupted over time.

**Fix.** All movement feeds (`getRecentMovements()`, employee shop movements, live
empty-cans totals) now use `limit(100000)`, effectively unbounded for this business
while overriding PostgREST's default 1000-row cap.

### 1.3 Analytics stock activity counted empty-can returns as real stock

**Symptom.** Inward/outward counts on the analytics screens included customer empty-
can returns and direct entries, inflating the "case-wise" numbers.

**Fix.** Inward/outward sums now exclude movement sources matching
`isEmptyCansSource()` in `ProfitDashboardScreen`, `SalesSummaryScreen` and
`DashboardMetrics.netInwardForMonth()` / `totalForType()`.

### 1.4 Stock History showed employee IDs instead of names

**Symptom.** After deleting an employee, the Stock History employee filter and
detail sheet rendered a raw id (`id: 80983839928299292992`) instead of a name.

**Fix.** Only resolvable employees are offered as filter chips (never raw ids), and
the activity detail sheet no longer mislabels unknown ids as "Admin".

### 1.5 Deleted employees vanished from reports

**Symptom.** When an employee was deleted, their past transactions (Report Summary)
showed a blank/em-dash employee, and stock history lost the attribution.

**Root cause.** `deleteEmployee()` hard-deleted the `employees` row, so nothing could
resolve `employee_id` → name afterwards.

**Fix.** Employees are now **soft-deleted** (`status = "inactive"`) while the auth
user is still disabled. `AdminViewModel` keeps the row in state as inactive; the
Employees screen hides inactive staff from the roster/counts but history lookups keep
resolving the name.

### 1.6 Report Summary values did not reconcile between sections

**Symptom.** The Transactions and By Product sections showed different profit /
margin / sale figures.

**Fix.** Both views now present **Cost | Sales | Profit** using identical math
(`cost = purchasePrice × qty`, `sales = totalSelling`, `profit = totalMargin`) with the
same margin-% formula, so the sum of any product's transactions equals its By Product
card exactly.

---

## 2. Improvements

### 2.1 Report Summary — Transactions, redesigned
Each transaction card now shows:
- **Date** (top-left) and **margin %** chip (top-right)
- **Customer name** (bold), **product · qty**
- **Cost | Sales | Profit** row (matching the By Product cards)
- **Employee name** and **shop name** (footer with icons)

By Product cards additionally display a **margin %** next to units sold.

### 2.2 Rename propagation (data consistency)
- **Customer rename** now rewrites all denormalized copies:
  `sale_entries.customer_name` and `stock_movements.source`
  (`"Sale · X"`, `"Empty cans · X"`, `"Empty cases · X"`), scoped to the tenant via
  `admin_id`. UI state is reloaded immediately after the rename.
- **Employee rename** now also syncs `profiles.full_name` (the source of the
  employee's display name in their own app / auth state), so the change is visible
  everywhere after the next session refresh.

### 2.3 Empty Cases quick-add
- Admin home: quick-add card + stepper dialog (`EmptyCasesStepperDialog`).
- Employee home: full-width "Add Empty Cases" card + same dialog; live empty-cases
  figure computed from shop movements minus the baseline.

### 2.4 Text-input capitalization
New `TextUtil.capitalizeWords()` helper + `KeyboardCapitalization.Words` applied
app-wide (employees, add-employee, customers, products, suppliers, shop name,
brand name, expense name, search fields). Decimal/numeric and email fields excluded.

### 2.5 Expenses
- Individual expense rows can be deleted.
- The prior month's expense is loaded for comparison when jumping between months.

---

## 3. New Files / Components

| File | Purpose |
|---|---|
| `shared/.../ui/StockHistoryScreen.kt` | New stock-ledger screen (search, shop/employee/date filters, sort) |
| `shared/.../ui/EmptyCasesStepperDialog.kt` | Stepper used by admin & employee quick-add |
| `shared/.../util/TextUtil.kt` | `capitalizeWords()`, `KeyboardType.isTextField()` |
| `shared/.../util/DateTimeUtil.kt` | Shared date/time display helpers |

---

## 4. Schema / Database

Run in the Supabase SQL Editor before relying on the Empty Cases reset:
- `app_settings` table (tenant-isolated baseline storage)
- `migrate_to_tenant_table('app_settings')`

See `supabase_schema.sql`.

---

## 5. Verification

Local checks used for this release:

```
JAVA_HOME="/Applications/Android Studio Preview 2.app/Contents/jbr/Contents/Home" \
  ./gradlew :shared:compileKotlinJvm :shared:jvmTest :desktopApp:compileKotlin
./gradlew :androidApp:assembleProdRelease
```

- `:shared:jvmTest` — all unit tests pass.
- `:androidApp:assembleProdRelease` — signed `androidApp-prod-release.apk` (24 MB).

---

## 6. Release Artifacts

- APK: `androidApp/build/outputs/apk/prod/release/androidApp-prod-release.apk`
- GitHub release: https://github.com/noorulakbar8098/Ruwia/releases/tag/v2.0