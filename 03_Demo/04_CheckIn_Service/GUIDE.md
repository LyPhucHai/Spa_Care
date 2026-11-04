# Demo 4 – Check-in and service (UC-06)

**What it proves:** an appointment follows a strict status machine CONFIRMED → CHECKED_IN → IN_PROGRESS → COMPLETED; steps cannot be skipped or repeated.
**Time:** about 1.5 minutes.

## Part 1 – UI (`UI/index.html`)
1. Click Mai's appointment (CONFIRMED, teal).
2. Click **Check in** → CHECKED_IN (blue). Click **Start service** → IN_PROGRESS (amber). Click **Complete service** → COMPLETED (green).
3. Point out that each status has its own color and that the button label always shows the only valid next step.

## Part 2 – Java (`Java/`)
Command: `./run.sh` (class `ServiceDemo`). Expected: appointment #1 created (CONFIRMED); jumping IN_PROGRESS → COMPLETED is **[BỊ TỪ CHỐI]**; then Check-in, Start and Complete each print the updated list; repeating check-in is **[BỊ TỪ CHỐI]**; final status COMPLETED.
