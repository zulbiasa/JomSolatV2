# JomSolat App

![Wear OS](https://img.shields.io/badge/Platform-Wear%20OS-blue) ![Kotlin](https://img.shields.io/badge/Kotlin-1.8-orange) ![License](https://img.shields.io/badge/License-MIT-green) ![Origin](https://img.shields.io/badge/Origin-Malaysia-yellow)

![JomSolat GIF](https://via.placeholder.com/400x300.gif)
*Interactive digital watch face for prayer times for Malaysian*

---

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Screenshots](#screenshots)
* [Architecture](#architecture)
* [Installation](#installation)
* [Usage](#usage)
* [Technologies Used](#technologies-used)
* [Contributing](#contributing)
* [License](#license)

---

## Overview

**JomSolat** is a Wear OS app designed to help Muslims track daily prayer times in Malaysia using API from JAKIM. The watch face shows current and next prayer times, location, and integrates Wear OS **complications** for step count, battery, and date.

It is optimized for **performance, accuracy, and simplicity**, providing glanceable information right on your wrist.

---

## Features

* Displays **current prayer**, **next prayer**, and **location**.
* Fully **interactive digital watch face** for Wear OS.
* **Fix Complications**:
  * Left: Step Count
  * Right: Battery
  * Top: Date
* Retrieves prayer times based on **GPS location**.
* Stores prayer times using **DataStore** for offline access.

---

## Screenshots

| Watch Face                                         | Prayer Details                                         |
| -------------------------------------------------- | ------------------------------------------------------ |
| ![Watch Face](https://github.com/zulbiasa/JomSolatV2/blob/main/app/src/main/res/drawable/watchface_preview.png) | ![Prayer Details](https://github.com/zulbiasa/JomSolatV2/blob/main/app/src/main/res/drawable/app_preview.png) |


---

## Architecture

JomSolat uses a **modular architecture**:

* **UI Layer**: Custom watch face renderer using `CanvasRenderer2`.
* **Data Layer**: `PrayerTimesDataStore` for saving prayer times and location.
* **Wear OS Integration**: `ComplicationSlotsManager` for step count, battery, and date.
* **Time Management**: ZonedDateTime and DateTimeFormatter for accurate display.

**Data Flow Diagram:**

```
GPS -> PrayerTimesDataStore -> WatchFaceRenderer -> CanvasRenderer2 -> Display
```

---

## Installation

### Prerequisites

* Android Studio Arctic Fox or newer
* Wear OS emulator or device
* Kotlin 1.8+
* Minimum SDK: 30

### Steps

```bash
git clone https://github.com/<your-username>/JomSolat.git
cd JomSolat
```

1. Open the project in **Android Studio**.
2. Build and run on a **Wear OS device** or emulator.
3. Grant **location permissions** for prayer time calculation.
3. Grant **Sensor & Activity permissions** for Steps Counter compilation.
---

## Usage

1. Launch JomSolat on your Wear OS device.
2. Allow location access.
3. The watch face displays:

   * Current prayer
   * Next prayer
   * Location
   * Complications (Steps, Battery, Date)
4. Prayer times update daily based on your GPS location.

---

## Technologies Used

* **Kotlin 1.8+** – Primary language
* **Android Studio** – IDE
* **Wear OS SDK** – Watch face & complications
* **DataStore** – Persistent storage
* **Jetpack Libraries** – Coroutines, Flow
* **CanvasRenderer2** – Custom rendering

---

## Contributing

1. Fork the repo.
2. Create a branch: `git checkout -b feature-name`
3. Commit your changes: `git commit -m "Add new feature"`
4. Push: `git push origin feature-name`
5. Create a **Pull Request**.

---


## License

This project is licensed under the **MIT License** – see [LICENSE](LICENSE) for details.
