# Android Launcher & Admin System - Project Plan

## Project Overview
Building a custom Android Launcher with a companion SvelteKit Admin Dashboard for parental control and device management. The system includes device registration, telemetry tracking (offline-first), user management, and policy enforcement.

---

## ✅ Completed Phases

### Phase 1: Foundation & Device Registration (v0.1.0 - v0.6.0)
- **Backend**:
  - [x] Project setup with SvelteKit & SQLite (Drizzle).
  - [x] Device Registration API with 5-digit temporary codes.
  - [x] Admin Dashboard for generating codes and viewing devices.
  - [x] **Update**: Switched to short integer Device IDs (v0.6.0).
- **Android**:
  - [x] Basic Jetpack Compose Launcher.
  - [x] Registration UI with code input.
  - [x] Secure storage of Device ID and Description.
  - [x] **Update**: Reset Registration debug button (v0.6.0).

### Phase 2: Authentication & User Management (v0.2.0)
- **Backend**:
  - [x] User Management UI (Create/Delete users, Roles: Parent/Child/Admin).
  - [x] Auth APIs (Login/Logout).
- **Android**:
  - [x] Login Screen & Session Manager.
  - [x] Persist user session and handle logout.

### Phase 3: Telemetry & Offline Sync (v0.3.0 - v0.4.0)
- **Backend**:
  - [x] Telemetry Ingestion API (Batch support).
  - [x] Real-time Telemetry Dashboard (GPS, App Usage).
- **Android**:
  - [x] `TelemetryWorker` (WorkManager) for background data collection.
  - [x] GPS Location & App Usage Stats collection.
  - [x] **Offline-First**: Room Database implementation for caching events when offline.
  - [x] Auto-sync when network is restored.

---

## 🚀 Current Focus: Phase 4 - Policy Management (In Progress)

This phase focuses on defining rules (policies) for devices/users and enforcing them on the Android client.

### 4.1 Backend Implementation ✅ COMPLETED
- [x] **Database Schema**:
    - Created `policies` table: `id`, `name`, `config` (JSON), `createdAt`.
    - Created `device_policies` table: `deviceId`, `policyId`, `assignedAt`.
- [x] **Policy Management UI**:
    - Page to Create/Edit Policies at `/policies`.
    - JSON Editor for policy config (Allowed Apps, System Toggles).
    - UI to assign a policy to a registered device.
- [x] **API Endpoints**:
    - `GET /api/policies`: List all policies.
    - `POST /api/policies`: Create new policy.
    - `POST /api/devices/:id/policy`: Assign policy to device.
    - `GET /api/sync/:deviceId`: Endpoint for Android to fetch latest policy.

### 4.2 Android Client Implementation (In Progress)
- [x] **Device Owner Setup**:
    - Created `LauncherAdminReceiver` (DeviceAdminReceiver).
    - Created `device_admin.xml` configuration.
    - Registered receiver in AndroidManifest.xml.
- [x] **Kiosk Manager**:
    - Implemented `KioskManager` helper class.
    - Methods: `enableKioskMode()`, `setAllowedApps()`, `setSystemRestrictions()`.
- [x] **PIN System**:
    - Created `PinManager` with EncryptedSharedPreferences.
    - Created `PinSetupScreen` UI (shown after first login).
    - Created `PinLockScreen` UI (shown when launching apps).
    - Integrated PIN flow into MainActivity.
- [ ] **Policy Sync & Enforcement**:
    - [ ] Update `TelemetryWorker` to fetch policy from `/api/sync/:deviceId`.
    - [ ] Store policy locally (Room or Prefs).
    - [ ] **Kiosk Logic**: Filter app drawer based on "Allowed Apps" list.
    - [ ] **Enable Kiosk Mode**: Call `startLockTask()` in MainActivity.onResume().

### Next Steps
1. Set Device Owner: `adb shell dpm set-device-owner com.example.launcher/.admin.LauncherAdminReceiver`
2. Implement policy sync in TelemetryWorker
3. Enable Kiosk mode on app launch
4. Filter app drawer based on synced policy
5. End-to-end testing

---

## 🔮 Future Phases

### Phase 5: Remote Commands & Kiosk Mode
- [ ] **Remote Lock/Wipe**: Admin triggers commands via FCM or polling.
- [ ] **Kiosk Mode**: Lock the launcher as the default home app with no exit.

### Phase 6: Advanced Analytics
- [ ] **Usage Reports**: Weekly/Monthly summaries of screen time.
- [ ] **Location History**: Map view of device movement over time.

---

## 📚 Documentation References
- `CHANGELOG.md`: Detailed history of implemented features.
- `docs/device-registration.md`: Specification of the registration flow.