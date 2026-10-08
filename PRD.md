# Enhanced Vehicle Management System — Product Requirements Document

Draft v1.0 · 7 October 2026

## 1. Overview

**Purpose.** A desktop application for a personal garage owner to record vehicles, maintenance, fuel and other expenses, and to be reminded of upcoming service and document expiries.

**Target user.** One owner, one computer, offline. No accounts, no network, no multi-user access.

**Problem.** Vehicle details, service receipts, fuel spending and renewal dates are scattered across notes, photos and memory. The owner cannot easily answer: what does this car cost me per kilometre, and what is due soon?

**Application name.** GarageLog (descriptive title: Enhanced Vehicle Management System).

## 2. Goals and Non-Goals

**Goals**

- Reliable CRUD for vehicles, with the plate number as the user-facing identifier.
- Track maintenance, expenses and detailed fuel fill-ups per vehicle.
- Show what is due (service, insurance, road tax) on a startup dashboard.
- Report total cost of ownership, spending trends and fuel consumption.
- Never lose or corrupt user data.

**Non-goals**

- Rentals, customers, availability or booking (earlier scope, now removed).
- Login, roles or multi-user access.
- Cloud sync, mobile app, online lookups or integrations.

## 3. Technical Decisions

- **Language/platform:** Java 25 (current LTS), desktop application. Every dependency uses its latest stable release at project start, never a beta.
- **UI:** JavaFX 25 (pairs with JDK 25). Swing is the fallback if zero external dependencies is required.
- **Build tool:** Maven 3.9.x (latest stable; Maven 4 is still at release-candidate stage). Chosen for its simple, declarative setup, abundant learning material, and because this is a small single-module project where Gradle's extra flexibility adds no value.
- **Storage:** a single JSON file, read and written with Gson (latest stable release).
- **Testing:** JUnit Jupiter (latest stable) for the service and repository layers.
- **PDF export (late phase):** OpenPDF (latest stable release), building the vehicle history report with tables and chart images snapshotted from JavaFX. Chosen over JavaFX printing because it writes a real PDF file with no print dialog, and over lower-level PDFBox because table and text layout is far simpler.
- **Defaults:** currency MYR, distance in km, fuel in litres. All configurable in Settings.

## 4. Architecture

Four separated layers. Each depends only on the layer beneath it.

- **Model:** plain data classes and enums (Vehicle, MaintenanceRecord, Expense, FuelFillUp, Settings).
- **Repository:** the only code that touches the JSON file. Handles load, atomic save and backups.
- **Service:** business rules, validation, calculations, reminder logic. Throws custom exceptions.
- **UI:** JavaFX screens. Displays data and collects input. Contains no business rules.

Suggested packages: `model`, `repository`, `service`, `ui`, `exception`, `util`.

Vehicle type is an enum (Car, Motorcycle, Truck/Van, Other). A subclass hierarchy is not needed and would complicate the JSON.

## 5. Data Model

**Vehicle**

- Internal ID (UUID, immutable) — the real key for all links.
- Plate number (required, unique after normalisation, editable)
- Nickname (optional), brand, model, year, type, colour, fuel type
- VIN (optional), current odometer, purchase date, purchase price
- Status: Active, In storage, Under repair, Sold
- Insurance expiry date, road tax expiry date
- Photo path (optional), notes (optional)

**MaintenanceRecord**

- ID, vehicle ID, date, odometer, work done, cost, workshop
- Next service due: by date and/or by odometer (both optional)
- Receipt/document paths (optional)

**Expense**

- ID, vehicle ID, date, category, amount, description
- Categories (fixed list): Fuel, Parking, Toll, Insurance, Road tax, Repairs, Other
- Receipt/document paths (optional)

**FuelFillUp**

- ID, vehicle ID, date, odometer, litres, price per litre, total cost
- Full tank (yes/no)
- Each fill-up counts toward the Fuel category in spending reports.

**Settings**

- Currency, distance unit, reminder lead time (days and km), monthly budget (optional)

**File-level**

- Schema version number at the top of the JSON file, for future migrations.
- Money stored as strings and handled as BigDecimal. Dates stored as ISO-8601 (LocalDate).

## 6. Functional Requirements

Priority: **P1** core, **P2** important, **P3** extra.

### 6.1 Vehicle management (P1)

- **FR-1 View vehicles:** list showing plate, nickname, brand/model, year, status, odometer. Sold vehicles hidden by default with a toggle to show them.
- **FR-2 Empty state:** when no vehicles exist, show "No vehicles yet" with an **Add Vehicle** button.
- **FR-3 Add vehicle:** form with validation. Reject duplicate plates.
- **FR-4 Edit vehicle:** select by plate, form pre-filled with current values, cancel supported, re-validated on save. Plate is editable; links use the internal ID so history is unaffected.
- **FR-5 Mark as sold:** archives the vehicle and keeps its full history.
- **FR-6 Delete vehicle:** permanent removal, separate from Sold. Requires a confirmation dialog stating how many linked records will also be deleted.
- **FR-7 Exit:** closes cleanly, releases resources. All changes are already saved (see FR-26).

### 6.2 Maintenance (P1)

- **FR-8** Add, view, edit and delete maintenance records per vehicle.
- **FR-9** Optional next-service-due by date and/or odometer.

### 6.3 Expenses (P1)

- **FR-10** Add, view, edit and delete expenses per vehicle, using the fixed category list.

### 6.4 Fuel log (P2)

- **FR-11** Add, view, edit and delete fill-ups.
- **FR-12** Calculate consumption (L/100 km or km/L) between consecutive **full-tank** fill-ups. Partial fill-ups are included in cost but not in the consumption calculation.
- **FR-13** Show consumption trend per vehicle.

### 6.5 Search, filter, sort (P2)

- **FR-14** Search vehicles by plate, nickname, brand or model.
- **FR-15** Filter by status, type and fuel type.
- **FR-16** Sort by plate, brand, year or odometer.
- **FR-17** Search and filter within a vehicle's maintenance, expense and fuel records (date range, category).

### 6.6 Reminders and dashboard (P2)

- **FR-18** Startup dashboard listing: services due soon or overdue (by date or km), insurance and road tax expiring soon or expired.
- **FR-19** Reminder lead time configurable in Settings (proposed defaults: 30 days, 500 km).
- **FR-20** Dismiss or snooze individual reminders.

### 6.7 Reports and charts (P2/P3)

- **FR-21 (P2)** Total cost of ownership per vehicle: purchase price plus all maintenance, expenses and fuel. Show cost per km and per month.
- **FR-22 (P2)** Spending by month, year and category, shown as charts.
- **FR-23 (P3)** Monthly budget with a warning when the month's total exceeds it.
- **FR-24 (P3)** Printable/PDF vehicle history report (useful when selling).

### 6.8 Data management (P2)

- **FR-25** CSV export of vehicles, maintenance, expenses and fuel. CSV import of vehicles with a row-by-row error report; duplicates skipped and reported.
- **FR-26** Auto-save after every successful change using atomic writes.
- **FR-27** Backup and restore (see section 8).

### 6.9 Extras (P3)

- **FR-28** Receipt/document attachment by file path on records and for insurance/registration. Missing files are handled gracefully.
- **FR-29** Vehicle photo (path) and notes field.
- **FR-30** Settings screen (currency, units, lead times, budget).

## 7. Business Rules and Validation

- **Plate normalisation:** trim, uppercase, remove spaces and hyphens before comparing. Uniqueness is checked on the normalised form.
- **Required fields (vehicle):** plate, brand, model, year, type, status.
- **Year:** between 1900 and next calendar year.
- **Money and quantities:** amounts, litres and prices must be positive. Odometer must be zero or more.
- **Dates:** not unreasonably far in the future. Service-due date cannot precede the record date.
- **Odometer rule:** a new record's odometer should not be lower than the previous record's for that vehicle. If it is, show a warning and let the user confirm to override (odometers get replaced and typos happen). The vehicle's current odometer follows the highest valid reading.
- **Delete rules:** permanent delete cascades to all linked records, after explicit confirmation.
- **Empty results:** search/filter with no matches shows a distinct "no matching results" message.
- **Errors:** custom exceptions (VehicleNotFound, DuplicatePlate, ValidationError, StorageError). The UI shows plain-language messages, never stack traces.

## 8. Non-Functional Requirements

- **Data safety:** save by writing to a temporary file, then replacing the real file. A crash mid-save must never corrupt existing data.
- **Backups:** automatic backup on startup, keeping the last 10. Manual export and restore available.
- **Corruption handling:** if the data file is missing or unreadable, say so, offer restore from backup, and never silently overwrite.
- **Schema migration:** older files are upgraded automatically using the version number.
- **Data location:** a fixed app folder in the user's home directory.
- **Logging:** errors and storage events written to a log file.
- **Performance:** instant for up to several thousand records. No special optimisation needed.
- **Usability:** keyboard-friendly forms, clear validation messages, confirmation on destructive actions, warning when closing a form with unsaved input.
- **Packaging:** runnable JAR (or native installer as a later option).

## 9. Screens

- **Dashboard:** reminders, quick stats, shortcuts.
- **Vehicle list:** search, filter, sort, add, empty state.
- **Vehicle detail:** summary plus tabs for Maintenance, Expenses, Fuel, Documents, and Cost summary. Edit, Mark as sold and Delete actions.
- **Reports:** spending charts, total cost of ownership, consumption trends, export.
- **Settings.**

## 10. Roadmap and Acceptance Criteria

1. **Core:** vehicle CRUD, validation, plate uniqueness, empty state, JSON storage, atomic save, backup. *Done when:* all vehicle operations work, invalid input is rejected with clear messages, and killing the app mid-save leaves a valid file.
2. **Search, filter, sort.** *Done when:* all combinations return correct results, including the empty-result message.
3. **Maintenance records.** *Done when:* full CRUD works and the odometer rule is enforced.
4. **Expenses and categories.** *Done when:* full CRUD works and expenses roll up per vehicle.
5. **Reminders and dashboard.** *Done when:* due and overdue items by date and km appear correctly and can be snoozed.
6. **Fuel log, reports and total cost of ownership.** *Done when:* consumption matches hand-calculated examples and totals reconcile with underlying records.
7. **CSV import/export, PDF report, budget alert, attachments, photo/notes, settings.**

Unit tests are written alongside each phase, not at the end.

## 11. Testing Strategy

- JUnit tests for validation, plate normalisation, duplicate detection, odometer rule, consumption calculation, cost totals, reminder logic, and repository save/load (including corrupted-file and migration cases).
- Manual test checklist per phase for the UI.
- Test data set with a few vehicles and a year of sample records.

## 12. Risks

- **Scope size:** the feature list is large. Mitigation: strict phase order; do not start a phase until the previous one passes its acceptance criteria.
- **Data loss:** mitigated by atomic save, backups and schema versioning.
- **Fuel calculation errors:** partial fill-ups distort consumption. Mitigated by the full-tank rule.
- **JavaFX setup friction:** JavaFX is a separate dependency. Mitigated by configuring it in Maven early.
- **PDF export:** needs an extra library. Kept in the final phase so it cannot block earlier work.
