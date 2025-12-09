# 📦 Archived Documentation

> **Purpose:** Historical documents kept for reference  
> **Status:** Superseded by `/docs/` folder

---

## 📋 WHY ARCHIVED?

These documents were created during development/review process and have been **consolidated** into the main documentation folder (`/docs/`).

They are kept here for:
- ✅ Historical reference
- ✅ Audit trail
- ✅ Backup in case needed

**⚠️ DO NOT USE** - Refer to `/docs/` for current documentation.

---

## 🗂️ ARCHIVED FILES

### Root Level (Moved from `/`)
- `CRITICAL_ISSUES_REVIEW.md` - Initial code review findings
- `BOOKING_PAYMENT_FLOW_REDESIGN.md` - Flow redesign document
- `IMPLEMENTATION_GUIDE.md` - Step-by-step implementation guide
- `FLOWS.md` - Original flow documentation
- `ISSUES.md` - Issue tracking (old format)
- `SEQUENCE_DIAGRAMS.md` - Sequence diagrams
- `API_REFACTORING_COMPLETE.md` - API refactoring notes
- `API_RESTRUCTURE_PLAN.md` - API restructure planning

### Main-Docs Folder
- `API_TESTING_GUIDE.md`
- `API_USAGE_EXAMPLE.md`
- `BOOKING_SEQUENCE_DIAGRAM.md`
- `BOOKING_SYSTEM_GUIDE.md`
- `EXCEPTION.md`
- `IMPLEMENTATION_FIXED.md`
- `JWT_AUTHENTICATION_GUIDE.md`
- `Movie_Booking_System_API_Tests.postman_collection.json`
- `Movie_Booking_System_Environment.postman_environment.json`
- `REVIEW_REPORT.md`
- `TESTING_GUIDE.md`

---

## ✅ NEW DOCUMENTATION STRUCTURE

All content has been **consolidated and improved** in `/docs/`:

```
docs/
├── README.md                 # Documentation index
├── 01-SETUP-GUIDE.md         # Setup & installation
├── 02-API-DOCUMENTATION.md   # API reference (TODO)
├── 03-AUTHENTICATION.md      # JWT auth guide
├── 04-BOOKING-FLOW.md        # ✅ Booking flow (FIXED)
├── 05-PAYMENT-FLOW.md        # ✅ Payment flow (FIXED)
├── 06-TODO.md                # Pending improvements
└── 07-TESTING-GUIDE.md       # Testing guide (TODO)
```

---

## 📍 MAPPING OLD → NEW

| Old File | New Location | Status |
|----------|--------------|--------|
| `CRITICAL_ISSUES_REVIEW.md` | `docs/06-TODO.md` | ✅ Consolidated |
| `BOOKING_PAYMENT_FLOW_REDESIGN.md` | `docs/04-BOOKING-FLOW.md` + `docs/05-PAYMENT-FLOW.md` | ✅ Split & improved |
| `FLOWS.md` | `docs/04-BOOKING-FLOW.md` | ✅ Rewritten |
| `Main-Docs/JWT_AUTHENTICATION_GUIDE.md` | `docs/03-AUTHENTICATION.md` | ✅ Improved |
| `Main-Docs/TESTING_GUIDE.md` | `docs/07-TESTING-GUIDE.md` | ✅ Enhanced |
| `IMPLEMENTATION_GUIDE.md` | `docs/06-TODO.md` | ✅ Converted to task list |

---

## ⚠️ IMPORTANT

**If you need historical context**, you can reference these files, but:

1. **Always check `/docs/` first** - It has the latest information
2. **These files may be OUTDATED** - Code has been fixed since they were written
3. **Use for comparison only** - To see what changed

---

**Last archived:** November 11, 2025
