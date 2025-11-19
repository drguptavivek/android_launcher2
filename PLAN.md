# Android Launcher & Admin System - Project Plan

## Project Overview
Building a custom Android Launcher with a companion SvelteKit Admin Dashboard for parental control and device management. The system includes device registration, telemetry tracking (offline-first), user management, and policy enforcement.

---

## ✅ Completed Phases

### Phase 1: Foundation & Device Registration (v0.1.0 - v0.5.0)
- **Backend**:
  - [x] Project setup with SvelteKit & SQLite (Drizzle).
  - [x] Device Registration API with 5-digit temporary codes.
  - [x] Admin Dashboard for generating codes and viewing devices.
- **Android**:
  - [x] Basic Jetpack Compose Launcher.
  - [x] Registration UI with code input.
  - [x] Secure storage of Device ID and Description.

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

## 🚀 Current Focus: Phase 4 - Policy Management

### Backend (SvelteKit)
- [ ] **Database Schema**: Define tables for `policies` (time limits, app allow/block lists).
- [ ] **Policy UI**: Create an interface to assign policies to specific devices or users.
- [ ] **API**: Implement endpoints to create, update, and fetch policies (`GET /api/policies/:deviceId`).

### Android Client
- [ ] **Policy Sync**: Update `TelemetryWorker` or create a new worker to fetch latest policies periodically.
- [ ] **Enforcement Logic**:
  - Implement a service to monitor app usage against allowed limits.
  - Create a blocking overlay for restricted apps or time-out scenarios.
- [ ] **Local Storage**: Cache policies locally using Room (similar to telemetry).

---

## 🔮 Future Phases

### Phase 5: Remote Commands & Kiosk Mode
- [ ] **Remote Lock/Wipe**: Admin triggers commands via FCM or polling.
- [ ] **Kiosk Mode**: Lock the launcher as the default home app with no exit.
- [ ] **App Management**: Remote install/uninstall (requires system privileges or MDM).

### Phase 6: Advanced Analytics
- [ ] **Usage Reports**: Weekly/Monthly summaries of screen time.
- [ ] **Location History**: Map view of device movement over time.

---

## 📚 Documentation References
- `CHANGELOG.md`: Detailed history of implemented features.
- `docs/device-registration.md`: Specification of the registration flow.