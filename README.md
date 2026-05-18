# TripFlights ✈️

A Trip.com-inspired flight booking Android app built with Clean Architecture + MVVM. Search real flights via the Amadeus API, manage bookings through Firebase, and earn Trip Coins through a membership rewards system.

---

## Screenshots

> Add screenshots here after building the app.

---

## Features

- **Flight Search** — Search one-way or round-trip flights with origin/destination autocomplete, date picker, and passenger selector
- **Live Flight Data** — Powered by the Amadeus Self-Service API (sandbox)
- **Firebase Authentication** — Email/password login, Google Sign-In, and password reset
- **Booking Flow** — Passenger details → add-ons → seat selection → checkout → e-ticket confirmation
- **My Trips** — View upcoming and past bookings with full booking detail
- **Trip Coins Rewards** — Earn coins on every booking; multiplier scales with membership tier
- **Membership Tiers** — Silver → Gold → Platinum → Diamond → Diamond+ → Black Diamond
- **Price Alerts** — Set target prices for routes and get notified when fares drop
- **Profile Management** — Edit personal info, view tier status and coin balance

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt 2.51.1 |
| Navigation | Navigation Component 2.7.7 (single-Activity) |
| UI | Material Design 3, ViewBinding |
| Auth & DB | Firebase Auth + Firestore |
| Flight API | Amadeus Self-Service API (Retrofit + OkHttp) |
| Async | Kotlin Coroutines + StateFlow |
| Local Storage | DataStore Preferences |
| Image Loading | Glide |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |

---

## Project Structure

```
com.example.intprogactivity/
├── di/                         # Hilt modules (App, Network, Repository)
├── domain/
│   ├── model/                  # Domain entities (User, Flight, Booking, etc.)
│   ├── repository/             # Repository interfaces
│   └── usecase/                # Business logic use cases
├── data/
│   ├── remote/
│   │   ├── amadeus/            # Amadeus API client, token manager, DTOs, mapper
│   │   └── firebase/           # Firestore data sources
│   ├── local/                  # Local flight/airport data, DataStore prefs
│   └── repository/             # Repository implementations
├── presentation/
│   ├── auth/                   # Login, Register, Forgot Password
│   ├── home/                   # Home search widget, deals, airport search
│   ├── search/                 # Search tab, results list, flight detail
│   ├── booking/                # Passenger details, add-ons, seat map, checkout, confirmation
│   ├── trips/                  # My Trips (upcoming/past), booking detail
│   ├── profile/                # Profile, price alerts
│   └── rewards/                # Trip Coins history
└── util/                       # Result, UiState, Extensions, Constants
```

---

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17 (bundled with Android Studio)
- A Firebase project
- An Amadeus Self-Service API account

### 1. Clone the repository

```bash
git clone https://github.com/your-username/INTPROGACTIVITY.git
cd INTPROGACTIVITY
```

### 2. Firebase setup

1. Go to the [Firebase Console](https://console.firebase.google.com) and create a new project.
2. Add an Android app with package name `com.example.intprogactivity`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Enable **Email/Password** and **Google** sign-in providers under Authentication.
5. Create a Firestore database in **test mode** to start.

### 3. Amadeus API setup

1. Register at [developers.amadeus.com](https://developers.amadeus.com) and create a Self-Service app.
2. Copy your **Client ID** and **Client Secret**.
3. Add them to `local.properties` (create the file if it doesn't exist):

```properties
AMADEUS_CLIENT_ID=your_client_id_here
AMADEUS_CLIENT_SECRET=your_client_secret_here
```

> `local.properties` is already in `.gitignore` — your credentials will not be committed.

### 4. Build and run

Open the project in Android Studio and click **Run**, or build from the terminal:

```bash
./gradlew assembleDebug
```

---

## Membership Tiers

| Tier | Requirement | Coin Multiplier |
|---|---|---|
| Silver | Default on registration | 1.0× |
| Gold | 1+ booking | 1.2× |
| Platinum | 3+ bookings in 12 months | 1.5× |
| Diamond | 8+ bookings + $1,000 spend in 12 months | 2.0× |
| Diamond+ | $10,000+ spend in 12 months | 2.5× |
| Black Diamond | Invite-only (admin-set via Firestore) | 2.5× |

Base earn rate: **10 Trip Coins per $1 spent**.

---

## Booking Rules

- Guests cannot book — registration is required.
- Flights departing within 2 hours cannot be booked.
- Ryanair flights are excluded from search results.
- Cancellations are only allowed for confirmed bookings departing more than 24 hours away.

---

## Firestore Schema

```
users/{uid}
  email, displayName, phone, photoUrl,
  membershipTier, tripCoins, totalBookings, totalSpend,
  tierExpiryDate, createdAt

bookings/{bookingId}
  userId, pnr, status (CONFIRMED/CANCELLED/COMPLETED/PENDING),
  outboundFlightJson, returnFlightJson?,
  passengers, totalPrice, currency,
  addOns, tripCoinsEarned, createdAt, travelDate

price_alerts/{alertId}
  userId, origin, destination, targetPrice, isActive, createdAt

users/{uid}/coin_history/{txId}
  amount, type (EARNED/REDEEMED/EXPIRED/BONUS),
  description, bookingId?, createdAt
```

---

## License

This project is for educational purposes.
