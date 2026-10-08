# GarageLog 🚗

**Enhanced Vehicle Management System** — a personal desktop app for tracking vehicles, maintenance, expenses, fuel, and costs. Built with Java 24 + JavaFX 24, storing all data locally on your device.

---

## Features

- **Dashboard** — metric cards, 3-row scrollable reminder viewport, spending chart, recent activity
- **Vehicles** — full CRUD with search, filter by status/type/fuel, sort, empty state cards with one-click filter reset
- **Vehicle Detail** — tabbed view: Overview · Maintenance · Expenses · Fuel · Cost Summary with full action buttons, horizontal scrolling, and zero text truncation
- **Date Standardization** — uniform `DD/MM/YYYY` format across all input forms, tables, reminders, and CSV import/export
- **Reports** — 6-month stacked bar chart + TCO (Total Cost of Ownership) table
- **Data** — CSV export/import, backup management with one-click restore
- **Settings** — currency, distance unit, reminder lead times, monthly budget
- **Reminders** — insurance, road tax, maintenance date & odometer alerts with snooze/dismiss
- **Fuel tracking** — L/100km and km/L consumption calculated between consecutive full-tank fill-ups
- **Offline/local** — all data in `~/.garagelog/garagelog.json`, no account needed

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| UI | JavaFX 24 |
| Serialization | Gson 2.12.1 |
| Build | Maven 3.9+ |
| Tests | JUnit 5.11 (20 tests) |
| Theme | McLaren Light (Papaya Orange `#FF8000`) |

---

## Requirements

- **JDK 24** (tested on 24.0.1)
- **Maven 3.9+**

---

## Getting Started

```bash
# Clone
git clone https://github.com/dicson0108/GarageLog.git
cd GarageLog

# Run
mvn javafx:run
```

On first launch, sample data is seeded automatically so you can explore every feature immediately.

### Quick launch (Windows PowerShell)

```powershell
.\run.ps1
```

---

## Project Structure

```
GarageLog/
├── src/
│   ├── main/
│   │   ├── java/com/garagelog/
│   │   │   ├── model/          # Vehicle, MaintenanceRecord, Expense, FuelFillUp, Settings, AppData
│   │   │   ├── exception/      # GarageLogException hierarchy
│   │   │   ├── repository/     # GarageRepository interface + JsonGarageRepository (atomic save, backups)
│   │   │   ├── service/        # VehicleService, MaintenanceService, ExpenseService,
│   │   │   │                   # FuelService, ReminderService, SettingsService, ReportService
│   │   │   ├── ui/             # JavaFX views, dialogs, MainLayout, GarageLogApp
│   │   │   └── util/           # DateUtil, JsonUtil, CsvUtil, ValidationUtil, PlateNormalizer, SampleDataGenerator
│   │   └── resources/
│   │       └── css/
│   │           └── mclaren-theme.css
│   └── test/                   # 20 JUnit 5 tests
└── pom.xml
```

---

## Data Storage

| Path | Description |
|---|---|
| `~/.garagelog/garagelog.json` | Main data file |
| `~/.garagelog/backups/` | Auto-rotating backups (10 kept) |

Writes are atomic (written to `.tmp`, then moved) to prevent corruption on crash.

---

## Running Tests

```bash
mvn test
```

20 tests covering: date utilities (`dd/MM/yyyy`), plate normalizer, validation, repository (save/load/backup/restore/corrupt-file), vehicle service (CRUD/duplicate/search), fuel consumption, and UI instantiation.

---

## License

MIT
