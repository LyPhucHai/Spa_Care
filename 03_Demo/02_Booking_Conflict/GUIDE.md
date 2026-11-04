# Demo 2 – Booking and conflict detection (UC-04, UC-17)

**What it proves:** free slots come from shifts and existing bookings; a double booking is refused; touching bookings are allowed; staff skills and shifts are enforced; one appointment can hold several services.
**Time:** about 3 minutes.

## Part 1 – UI (`UI/index.html`)
1. The **Book** tab opens. Choose **Trần Mai**, tick **Body massage 90m**, staff **Lan (Spa)**, click slot **10:00**, then **Confirm booking** → toast "Appointment #3 confirmed" and the Appointments grid shows Mai under Lan.
2. Click **Book** again. Choose **Nguyễn Thu H.**, tick only **Relaxing hair wash 60m**, staff **Lan**. Slots 10:00–11:00 are crossed out.
3. Click **10:00** and **Confirm booking** → red toast "Conflict: Lan (Spa) is already booked at 10:00".
4. Choose **11:30** (touches the end of the massage) → confirmed.
5. Switch staff to **Minh** with Massage ticked → "not skilled" error; Minh has no slots after 14:00 (shift limit).

## Part 2 – Java (`Java/`)
Command: `./run.sh` (class `BookingDemo`). Expected, in order:
1. Customer list (Thu H. Gold 420 pts, Mai Silver 80, Linh Platinum 900).
2. Lan's free slots for a 90-minute massage (08:00, 08:30, …).
3. Mai, Massage, Lan, 10:00 → `#1`.
4. Thu H., wash, Lan, 10:00 → **[BỊ TỪ CHỐI]** conflict.
5. Thu H., wash, Lan, 11:30 → `#2`.
6. Massage with Minh → **[BỊ TỪ CHỐI]** no skill. 7. Haircut with Minh at 15:00 → **[BỊ TỪ CHỐI]** outside shift.
8. Wash + haircut (105 min) with Minh at 08:00 → `#3`. 9. Appointment list.
Say: "The conflict check runs again inside the transaction before saving."
