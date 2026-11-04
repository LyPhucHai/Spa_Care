# Demo 3 – Reschedule and cancel (UC-05)

**What it proves:** rescheduling respects conflicts, an appointment never blocks itself, cancelling frees the slot, and only CONFIRMED appointments can be changed.
**Time:** about 2 minutes.

## Part 1 – UI (`UI/index.html`)
The grid shows #1 Mai/Massage/Lan 10:00, #2 Thu H./Wash/Lan 14:00, #3 Linh/Haircut/Minh 09:00 (already CHECKED_IN).
1. Click Mai's appointment → panel with **Reschedule** and **Cancel appointment**.
2. Choose **14:00 (busy)** and click Reschedule → red toast "Conflict: Lan is already booked at 14:00".
3. Choose **16:00** → "moved to 16:00. Old slot released", grid updates.
4. Click Thu H.'s appointment → **Cancel appointment** → shows cancelled, slot freed.
5. Click Linh's appointment (CHECKED_IN) → no Reschedule/Cancel buttons: the status rule.

## Part 2 – Java (`Java/`)
Command: `./run.sh` (class `RescheduleDemo`). Expected: #1 and #2 created; move #1 to 14:00 → **[BỊ TỪ CHỐI]**; move #1 to 16:00 → ok; to 15:30 → ok; cancel #2 → ok; cancel #2 again → **[BỊ TỪ CHỐI]**; check-in #3 then cancel → **[BỊ TỪ CHỐI]**; final list: #1 at 15:30, #2 CANCELLED, #3 CHECKED_IN.
Say: "The 2-hour notice rule is the constant MIN_NOTICE_HOURS; reception and managers can override it."
