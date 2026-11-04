# Presentation and demo script (12–15 minutes + 5–8 minutes Q&A)

Assignment rules: every member presents at least one part; do not read the slides; the Java program must really run on the presenting machine; be ready to answer questions about requirements, design decisions, scalability and each member's own work.

For every demo: show the **UI prototype first** (what the user sees), then the **Java program** (the same rule running as real code). Exact clicks and expected results are in `03_Demo/0X_*/GUIDE.md`.

| Time | Presenter | Content | Where |
|---|---|---|---|
| 0:00–2:00 | Hai | Problem, scope, 19 FR / 9 NFR, MoSCoW | slides 1–4 |
| 2:00–4:00 | An | 17 use cases, ERD, booking sequence, loyalty rules, UI screens | slides 5–9 |
| 4:00–5:30 | Hai | Demo 1 – Login and roles (UC-01) | 03_Demo/01_Login |
| 5:30–8:00 | Hai | Demo 2 – Booking and conflict (UC-04, UC-17) | 03_Demo/02_Booking_Conflict |
| 8:00–9:30 | Hai | Demo 3 – Reschedule and cancel (UC-05) | 03_Demo/03_Reschedule_Cancel |
| 9:30–11:00 | An | Demo 4 – Check-in and service (UC-06) | 03_Demo/04_CheckIn_Service |
| 11:00–13:30 | An | Demo 5 – Payment, points, Gold upgrade (UC-07, UC-08) | 03_Demo/05_Payment_Loyalty |
| 13:30–14:30 | Hai | Results, limitations, future work | slides 11–12 |

Sample data (same for every demo): customers Thu H. (Gold, 420 points), Mai (Silver, 80 points, 9.5 million VND spent), Linh (Platinum, 900 points); staff Lan (all-day), Hà (all-day), Minh (morning only); five services (500,000 / 150,000 / 400,000 / 200,000 / 250,000 VND). Full list: report Table 7.3.

Before the session: open the five `UI/index.html` files in tabs; open a terminal and run each `run.sh` once (first run downloads H2); keep screenshots of each Java output as a backup. If time is short, shorten Demo 1 and Demo 3 first and keep Demos 2 and 5, which prove the hardest logic.
Hand-over lines: "Now I will show how a receptionist sees this…", "Now the same rule as real Java code…".
