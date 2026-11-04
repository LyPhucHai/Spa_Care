# Demo 5 – Payment, points and tier upgrade (UC-07, UC-08)

**What it proves:** tier discount, point redemption (100 points = 10,000 đ, max 50 % of the invoice), points earned with the tier multiplier, automatic upgrade, and guards against invalid payments.
**Time:** about 3 minutes.

## Part 1 – UI (`UI/index.html`)
The grid shows three COMPLETED appointments (Mai, Thu H., Linh) and one still CONFIRMED (Mai, nail).
1. Click **Mai's** massage (500,000 đ, SILVER, 80 points) → **Payment** block. Leave points 0, click **Pay and print invoice** → toast "Paid 500,000 đ. Upgraded to GOLD!" and the Customers tab shows Mai GOLD, 130 points.
2. Back to Appointments, click **Thu H.** (900,000 đ, GOLD): redeem **200** points → total 835,000 đ (−45,000 tier, −20,000 points), +100 points → balance 320.
3. Click **Linh** (150,000 đ, PLATINUM): enter **900** points → toast "Points can cover at most 50% of the invoice". Enter **600** → total 75,000 đ.
4. Open the still-CONFIRMED appointment: no payment block (not completed).

## Part 2 – Java (`Java/`)
Command: `./run.sh` (class `PaymentDemo`). Expected:
| Step | Result |
|---|---|
| A) Mai pays #1, 0 points | 500,000 đ, +50 points (130), spending 10,000,000 đ → **upgraded to GOLD** |
| B) Thu H. pays #2, 200 points | −45,000 tier, −20,000 points → 835,000 đ, +100 points (320), stays GOLD |
| C1) Linh redeems 900 points on #3 | **[BỊ TỪ CHỐI]** more than 50 % |
| C2) Linh redeems 600 points | −15,000 tier, −60,000 points → 75,000 đ, +11 points (311) |
| D1) pay #4 (not completed) | **[BỊ TỪ CHỐI]** |
| D2) pay #1 again | **[BỊ TỪ CHỐI]** already paid |
The program prints customers before and after so the change in tier and points is visible.
