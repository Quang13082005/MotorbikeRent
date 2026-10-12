# Browser verification — 2026-09-29

Target: final Phase 4 clone server http://127.0.0.1:8083, standard security, real payments disabled. Dedicated disposable order PHASE4-UI-20260929 id 116 for customer_ui_verify. Never used real payment or customer information.

Observed using the in-app Browser skill:

1. Customer login succeeded through normal form. Extension page displayed original return 30/09/2026 14:46 and total/paid 200000 VND.
2. Initial submit was blocked by native datetime-local validity: min contained 14:46:58 and default step=60 made 14:46 invalid. Fixed template step=1, rebuilt, restarted ONLY 8083, loaded a fresh tab after an obsolete connection-error tab could not navigate.
3. Customer entered 02/10/2026 14:46 and a test reason, submitted. Detail page showed one extension with 200000 VND and Chờ duyệt. Original schedule/total unchanged.
4. Logged out Customer, logged in demo Staff, opened /staff/extensions. One fixture row displayed date, fee and reason. Clicked Duyệt. The browser tool reported a click timeout around the native confirmation; fresh DOM afterward showed authoritative Chờ thanh toán gia hạn, deadline 15:06:14, no approval buttons. Did not repeat the action. Database inspect confirmed original return and total unchanged (fixture-after-approval.log).
5. Logged out Staff, logged back in Customer. Detail showed Chờ thanh toán gia hạn, fee 200000, deadline and Thanh toán phí gia hạn button, with explicit text that only successful payment changes the date.
6. Clicked payment. Live status message: PayOS đang tắt trên bản kiểm thử. No provider navigation/payment occurred. Inspected viewport/full-page screenshots and DOM; Vietnamese extension content and action appeared. Full-page capture stitching duplicated visual strips; DOM heading count was one. No claim of comprehensive responsive-layout acceptance.
7. Logged out Customer; observed /auth/login?logout and closed test tab. Dedicated JDBC helper cleaned fixture/extension/payment/notifications. Both database baselines checked afterward.

Not tested by this browser run: real provider success, public webhook networking, automated expiry refresh without reload, every viewport, conflict hotline UI. Backend conflict message/expiry/races and provider-mocked success are covered by named automated tests, not by this UI evidence.
