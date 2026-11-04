# SpaCare – Demo package (demo only)

Five functions, each in its own folder. Inside every folder the **UI** and the **Java** program are separate and independent, and a `GUIDE.md` lists the exact steps and expected results.

| Folder | Function | Use cases | Time |
|---|---|---|---|
| 01_Login | Login, roles, lockout | UC-01 | 1.5 min |
| 02_Booking_Conflict | Book appointment, conflict detection | UC-04, UC-17 | 3 min |
| 03_Reschedule_Cancel | Reschedule and cancel | UC-05 | 2 min |
| 04_CheckIn_Service | Check-in, start, complete service | UC-06 | 1.5 min |
| 05_Payment_Loyalty | Payment, points, tier upgrade | UC-07, UC-08 | 3 min |

```
0X_Function/
├── UI/index.html     open in any browser (clickable prototype of this function only)
├── Java/             stand-alone Maven project (JDK 17+): ./run.sh or run.bat
└── GUIDE.md          step-by-step demo script and expected results
```

- **UI demos** need nothing installed; data resets when the page is reloaded.
- **Java demos** each start a fresh in-memory H2 database and use tomorrow's date, so they run in any order and never depend on each other. Lines marked `[BỊ TỪ CHỐI]` are intended: they show a business rule refusing an invalid action.
- On Windows use `run.bat` (it switches the console to UTF-8). The first Maven run needs internet to download H2; run each once before the presentation.
- All five Java projects were compile-checked with JDK 21; the runtime output has not been captured yet, so take screenshots on your machine.
