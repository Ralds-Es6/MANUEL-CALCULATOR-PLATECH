# MANUEL Calculator (PLATECH)

Two Cyber Cyan Java calculators — available as **Android APKs** (installable side-by-side) and **Desktop Java Swing apps**.

## Download for Android

Click either button below to download the `.apk` directly:

| App | Features | Direct Download |
| :--- | :--- | :--- |
| **Cyber Mini Calculator** | Basic arithmetic (`+`, `−`, `×`, `÷`, `%`, `( )`, `DEL`, `AC`) with live result preview | [![Download Cyber Mini Calculator APK](https://img.shields.io/badge/Download-Cyber--Mini--Calculator.apk-00d8ff?style=for-the-badge&logo=android&logoColor=050d14)](https://github.com/Ralds-Es6/MANUEL-CALCULATOR-PLATECH/releases/latest/download/Cyber-Mini-Calculator.apk) |
| **Cyber Main Calculator** | Scientific (`sin`, `cos`, `tan`, `DEG`/`RAD`, `ln`/`log`, `x²`/`xʸ`/`√`, `n!`, `π`/`e`, `±`, `ANS`) | [![Download Cyber Main Calculator APK](https://img.shields.io/badge/Download-Cyber--Main--Calculator.apk-0ea5e9?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ralds-Es6/MANUEL-CALCULATOR-PLATECH/releases/latest/download/Cyber-Main-Calculator.apk) |

### Direct Links
- **Cyber Mini Calculator APK:** https://github.com/Ralds-Es6/MANUEL-CALCULATOR-PLATECH/releases/latest/download/Cyber-Mini-Calculator.apk
- **Cyber Main Calculator APK:** https://github.com/Ralds-Es6/MANUEL-CALCULATOR-PLATECH/releases/latest/download/Cyber-Main-Calculator.apk

### How to Install on Android
1. Tap one of the download buttons above on your Android phone.
2. Open the downloaded `.apk` file.
3. If prompted, allow **"Install unknown apps"** for your browser/files app, then tap **Install**.
4. Both apps have separate package IDs (`com.manuel.cybercalculator` and `com.manuel.maincalculator`), so you can install both on the same device.

---

## Run on Desktop (Linux / Windows / macOS)

Requires **Java 17+**:

```bash
# Cyber Mini Calculator (GUI)
java mini-calculator/CyberCalculator.java

# Cyber Main Scientific Calculator (GUI)
java main-calculator/MainCalculator.java
```

## Project Structure
- `mini-calculator/` — Desktop Java Swing Cyber Mini Calculator
- `main-calculator/` — Desktop Java Swing Cyber Main Scientific Calculator
- `android/` — Multi-module Android project (`:core`, `:mini-calculator`, `:main-calculator`)
- `.github/workflows/build-apks.yml` — Automatically builds both APKs and publishes them to GitHub Releases
