# Contributing to PrivaDoT

Thank you for your interest in contributing to **PrivaDoT**! We welcome contributions from developers, security researchers, and privacy advocates worldwide.

PrivaDoT is built on an unwavering foundation of **absolute privacy, zero telemetry, and complete offline autonomy**. Every contribution must adhere to these core values.

---

## 🛡️ Core Principles for Contributions

Before contributing code, please ensure your changes respect our non-negotiable architectural rules:

1. **Zero Network Communication:**
   - PrivaDoT does **not** declare or use `android.permission.INTERNET`.
   - **No** analytics SDKs (e.g. Firebase, Mixpanel, Crashlytics, Telemetry).
   - **No** background HTTP/WebSocket calls.

2. **Security & Privacy First:**
   - All persistence must remain encrypted at rest using local SQLCipher.
   - Any sensitive operations must fail securely.
   - Respect user resource usage (battery, CPU, memory).

3. **Modern Android Standards:**
   - 100% Kotlin with Coroutines and Flow.
   - Declarative UI via Jetpack Compose & Material 3 tokens.
   - Clean MVVM architecture.

---

## 🛠️ Development Setup

### Prerequisites
- **Android Studio Ladybug (2024.2.1+)** or newer
- **JDK 17** (Temurin or Android Studio JBR recommended)
- **Android SDK Platform 36**

### Getting Started
1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/PrivaDoT.git
   cd PrivaDoT
   ```
3. **Open** the project in Android Studio and let Gradle sync.
4. **Build and test** debug variant:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🌿 Branching & Commit Conventions

- Create a feature or bugfix branch off `main`:
  ```bash
  git checkout -b fix/indicator-alignment
  # or
  git checkout -b feat/custom-dot-colors
  ```

- Follow [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat:` A new feature
  - `fix:` A bug fix
  - `docs:` Documentation updates
  - `refactor:` Code change that neither fixes a bug nor adds a feature
  - `perf:` A code change that improves performance

---

## 🚀 Submitting a Pull Request (PR)

1. Ensure your code builds locally without errors (`./gradlew assembleDebug`).
2. Push your branch to your GitHub fork:
   ```bash
   git push origin fix/indicator-alignment
   ```
3. Open a **Pull Request** targeting the `main` branch of `InvisusNova/PrivaDoT`.
4. Fill out the Pull Request template completely with:
   - Clear description of the changes.
   - Verification/testing steps performed.
   - Screenshots/videos for any UI changes.
5. Wait for the automated GitHub Actions CI check to pass and project maintainers to review.

---

## 💬 Community & Support

Have questions or ideas?
- Open a [GitHub Issue](https://github.com/InvisusNova/PrivaDoT/issues) for bugs or feature requests.
- For sensitive vulnerability reports, refer to [SECURITY.md](SECURITY.md).
