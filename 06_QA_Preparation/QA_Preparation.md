# Likely questions and suggested answers

1. **Why these requirements?** They come from the problems found in the survey (double booking, no reminders, manual loyalty, slow reports); each FR traces to a use case (Table 4.2) and is prioritized with MoSCoW.
2. **Why MoSCoW?** It separates what the system cannot run without (booking, conflicts, payment) from differentiators (loyalty tiers, reminders) and lets us deliver a working core first.
3. **How do you prevent double booking?** Free slots are computed from shifts and existing bookings; the final check is repeated inside a transaction; the database design adds an overlap rule (EXCLUDE in PostgreSQL, row locking in MySQL).
4. **Why a separate appointment_service table?** One appointment can contain several services, each with its own staff, room, time and price at booking; this makes conflict checks, commissions and price history accurate.
5. **Why store price_at_booking?** Later price changes must not alter past appointments or invoices.
6. **How are points kept consistent?** point_balance is derived and updated in the same transaction that writes loyalty_transaction, which records balance_after for audit.
7. **Why these tier thresholds?** They are the group's assumptions, stored in membership_tier so the owner can change them without code changes.
8. **How does the system scale?** A branch table and branch-scoped rooms/appointments support several branches; the layered design lets the console UI be replaced by Web/Spring without touching business logic.
9. **What about security and privacy?** Role-based access, hashed passwords, lockout after 5 failures, HTTPS in production, personal data handled in line with Decree 13/2023/ND-CP. (The demo uses salted SHA-256; production should use bcrypt.)
10. **Why H2 for the demo?** It needs no installation; the SQL is plain, so MySQL works by changing the JDBC URL.
11. **What is not implemented?** Reminders and payment gateways are simulated, inventory/commission screens are design-only, JUnit tests are planned. Say this openly.
12. **Who did what?** Hai: Chapters 1–3, use case specs, login/booking/conflict code. An: Chapters 4–6, ERD/database, prototype, payment/loyalty code (see Appendix A; adjust to the real split).

13. **How are the UI and Java demos related?** Each function has a UI prototype (the designed screens, in-memory, in the browser) and a separate Java program (real code with a database enforcing the same rules), so every rule is shown twice: as designed and as implemented.
14. **Which use cases are implemented in Java?** UC-01, 04, 05, 06, 07, 08 (6 of the 7 specified, 86%). UC-10 (shifts and attendance) is specified; its sample shift data is loaded, screens are future work.
