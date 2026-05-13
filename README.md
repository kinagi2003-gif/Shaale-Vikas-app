# Shaale Vikas - Android Frontend

Shaale Vikas is a school-alumni bridge app for rural school infrastructure needs. This frontend prototype shows a transparent needs dashboard, simulated alumni pledges, admin editing, donor recognition, and impact photo proof.

## Open in Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select this folder:
   `C:\Users\bhuvi\Documents\Codex\2026-05-06\project-title-androidappdevelopmentusinggenai-shaale-vikas-education`
4. Let Gradle sync.
5. Run the app on an emulator or Android phone.

## Current Features

- Frontend-only demo login for Headmaster/Admin and Alumni roles.
- Needs Dashboard with professional card layout and progress bars.
- Simulated Pledge button. It records commitment only and does not process money.
- Headmaster Admin screen to add/edit needs, update collected amount, choose photos, and mark work completed.
- Donor Hall of Fame listing alumni pledges.
- Impact Photos screen for completed work and visual proof.

## Backend Login

- Create accounts from **New User** after Firebase is connected.
- Use role `Admin` for Headmaster/Admin accounts.
- Use role `Alumni` for alumni accounts.

The login screen has role buttons for Alumni and Headmaster/Admin. New User creates a Firebase Authentication account and stores role details in Firestore. Forgot Password sends a Firebase password reset email.

## Files To Edit First

- Main UI and logic:
  `app/src/main/java/com/example/shaalevikas/MainActivity.java`
- App name, colors, and theme:
  `app/src/main/res/values/`

## Firebase Backend Setup

The app now uses Firebase Authentication and Cloud Firestore. Firebase Storage is intentionally not required, so you do not need to upgrade billing for image uploads.

1. Open [Firebase Console](https://console.firebase.google.com/).
2. Create a project named `Shaale Vikas`.
3. Add an Android app with package name:
   `com.example.shaalevikas`
4. Download `google-services.json`.
5. Put it here:
   `app/google-services.json`
6. In Firebase Console, enable:
   - Authentication -> Email/Password
   - Firestore Database
7. Sync and run the app from Android Studio.

Important: The app will not build in backend mode until `app/google-services.json` exists.

## Backend Collections

- `users`
  - `name`
  - `email`
  - `role`: `Admin` or `Alumni`
  - `createdAt`
- `needs`
  - `title`
  - `category`
  - `description`
  - `estimate`
  - `collected`
  - `priority`
  - `completed`
  - `imageMode`
  - `createdAt`
  - `updatedAt`
- `pledges`
  - `needId`
  - `needTitle`
  - `name`
  - `note`
  - `amount`
  - `paymentMethod`
  - `paymentStatus`
  - `userId`
  - `createdAt`

## UPI Payments

The app has a no-cost **Pay via UPI** button. It opens any installed UPI app and saves the contribution as `Pending verification` in Firestore.

Before real use, replace this value in `MainActivity.java`:

```java
private static final String SCHOOL_UPI_ID = "schoolupi@bank";
```

Use the real school/trust UPI ID. Admin should verify UPI payments from bank/UPI statements before treating them as confirmed.

## Simple Demo Rules

Use these only for an internship demo. They require login but do not fully protect admin-only writes.

Firestore rules:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Photos are local preview only in this free backend version. They are not uploaded to Firebase Storage.
