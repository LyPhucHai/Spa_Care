# Demo 1 – Login and roles (UC-01)

**What it proves:** users authenticate with a hashed password, each role gets different permissions, and an account locks after 5 wrong attempts (NFR-02).
**Time:** about 1.5 minutes.

## Part 1 – UI (`UI/index.html`)
Open the file in a browser (no server needed).
1. Click **Log in** with `letan01` / `123456` → welcome card, role *Receptionist* and the list of functions it may use.
2. Click **Log out**, log in as `ktv_lan` → role *Technician*; red note "Technicians cannot take payment."
3. Log in as `ql_hoa` (Manager) → also reports and staff management.
4. Log out, enter `admin` with a wrong password five times → messages "Wrong password (1/5)" … "(5/5) Account is now locked".
5. Enter the **correct** password for `admin` → toast "Account locked after 5 failed attempts". An unknown username shows "Unknown username".

## Part 2 – Java (`Java/`)
Command: `cd Java` then `./run.sh` (Windows `run.bat`). Class: `LoginDemo`.
Expected: letan01 → RECEPTIONIST; ktv_lan → TECHNICIAN; ql_hoa → MANAGER; admin with a wrong password five times → five "sai mật khẩu"; then the correct password is **[BỊ TỪ CHỐI] account locked**; unknown user → null.
Say: "Only salted hashes are stored; the lockout counter is checked before the password."
