# Klypt: Offline-First Education for All ✨

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Empowering students and teachers with a fully offline, gamified learning experience—powered by Gemma 3n.**

**Klypt** is a lightweight, offline-first educational platform built to serve students and educators in areas with limited or unreliable internet access. Powered by Google’s **Gemma 3n** multimodal model and built using the AI Edge stack, Klypt reimagines how learning content is shared, accessed, and experienced—**entirely on-device**.

Our mission is to make interactive, gamified learning universally accessible, regardless of connectivity or infrastructure. Klypt addresses real-world challenges—teachers struggling to share content without the cloud, students without stable internet, and the absence of engaging offline learning tools—by combining modern AI, intuitive UX, and robust local-first infrastructure.

---

## ✨ Core Features

### 📦 Seamless Class Sharing

* Export/import class content using lightweight `.json` files—designed to be open, transparent, and developer-friendly.
* Students can **import class files and start quizzes instantly**, with zero need for internet or cloud-based authentication.
* Classes can be distributed via file transfer by educators, with no dependency on a class code or online syncing.
* Educators can **generate and share new classes locally**, without internet, immediately after downloading the model.

### 🔐 Offline-First Login System

* **Powered by Couchbase Lite**, enabling secure offline authentication and data persistence.
* Future support for **OTP verification** for educators to validate identity and recover accounts offline.
* Ensures privacy, accessibility, and full functionality even in zero-connectivity zones.

### 💾 Embedded Local Database

* Entire user experience is backed by **local-first data storage**, powered by Couchbase Lite.
* Future-ready for optional cloud sync, while defaulting to **user-controlled, private data**.

### 🏗️ Lightweight, Modular Architecture

* Built using **Kotlin** and **Jetpack Compose** for modern Android development.
* Integrated with **Google AI Edge stack**, using **Gemma 3n** to power on-device, multimodal AI experiences.
* Dynamically scales performance with **mix’n’match** submodel selection (2B, 4B, 8B).
* Engineered for modularity—future support for voice input, image-based learning, and multilingual prompts.

### 🎮 Gamified Learning Model

* Tracks quiz completion, scores, and learning progress to increase student motivation.
* Designed to make offline learning more interactive, rewarding, and autonomous.
* Planned expansion includes streaks, adaptive difficulty, and content mastery metrics.

---

## 🌍 A Real Problem, A Real Solution

One of our team members, Ahmed, tested the app with his younger siblings in a home with unreliable internet. With just a class file and the app, they were able to access quizzes and start learning—offline, instantly. This scenario reflects our target impact: bringing powerful AI-driven education to underconnected communities everywhere.

---

## 🎯 Aligned with Global Goals

Klypt supports the **UN Sustainable Development Goal 4: Quality Education**, which aims to "ensure inclusive and equitable quality education and promote lifelong learning opportunities for all."

---

## 🏁 Get Started in Minutes!

1. **Download the App**: [Coming soon – APK link]
2. **Install & Explore**: Clone the repository and run the app on any Android device.
3. **Load Class Files**: Open the app, load your `.json` class file, and start learning—no internet required.

---

## 🛠️ Technology Highlights

* **Google AI Edge** – For on-device LLM processing.
* **Gemma 3n** – Powering multimodal, privacy-first interactions.
* **Couchbase Lite** – Enabling full local-first data management and offline login.
* **Kotlin + Jetpack Compose** – Modern tools for native Android development.

---

## 🤝 Feedback

This is an experimental alpha release—we’d love your input!

* 🐞 **Found a bug?** [Report it here](https://github.com/yourusername/klypt/issues/new?assignees=&labels=bug&template=bug_report.md&title=%5BBUG%5D)
* 💡 **Have an idea?** [Suggest a feature](https://github.com/yourusername/klypt/issues/new?assignees=&labels=enhancement&template=feature_request.md&title=%5BFEATURE%5D)

---

## 📄 License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.

---

## 🔗 Useful Links

* [Demo Video (Kaggle Submission)](https://your-demo-video-link.com)
* [Technical Writeup](https://your-writeup-link.com)
* [Live Demo / App Site](https://your-app-demo-link.com)
