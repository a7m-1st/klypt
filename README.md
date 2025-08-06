# Klypt: Offline-First Education for All ✨

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Empowering students and teachers with a fully offline, gamified learning experience—powered by Gemma 3n.**

**Klypt** is a modular, offline-first educational platform built to serve students and educators in areas with limited or unreliable internet access. Powered by Google’s Gemma 3n multimodal model and built using the AI Edge stack, Klypt reimagines how learning content is shared, accessed, and experienced—entirely on-device.

Our mission is to make interactive, gamified learning universally accessible, regardless of connectivity or infrastructure. Klypt addresses real-world challenges—teachers struggling to share content without the cloud, students without stable internet, and the absence of engaging offline learning tools—by combining modern AI, intuitive UX, and robust local-first infrastructure.

---

## 1. Class Sharing

### Export/Import via File

**How it works:**  
Educators can export entire classes—including metadata, quizzes, and klyps—as `.json` files. These files use a transparent, open schema, so all class information is readable and editable outside the app. Students receive these files via any offline method (Bluetooth, USB, etc.), and can instantly import them into Klypt. Upon import, the app parses the JSON, reconstructs the class, and makes quizzes available for immediate play—no internet or cloud authentication required.

**Technical details:**  
- The export/import logic is implemented in the class management and repository layers.
- The JSON format is designed for maximum compatibility and transparency, allowing developers and users to inspect or modify content directly.
- Error handling ensures that corrupted or incomplete files are gracefully rejected, with clear feedback to the user.

### Class Code

**How it works:**  
While class code-based sharing is not currently supported, Klypt’s file-based approach allows educators to distribute classes securely and transparently. In the future, class codes may be used to facilitate online sync and sharing, but the current system prioritizes offline accessibility.

### Generate New Class

**How it works:**  
New accounts can generate classes locally after downloading the required AI model. The class creation flow is fully offline: educators input class details, add klyps and quizzes, and export the finished class as a JSON file. All operations are performed on-device, ensuring accessibility in any environment.

---

## 2. Offline-First Login

### Couchbase Lite Integration

**How it works:**  
Klypt uses Couchbase Lite to store user credentials and session data locally. When a user logs in, their information is validated against the local database. This enables secure, offline-first authentication, with all user data and progress stored on-device.

**Technical details:**  
- The login flow is implemented using Kotlin and Jetpack Compose, with type-safe navigation and state management.
- Future updates will allow optional synchronization with an online database, enabling educators to share materials and recover accounts when connectivity is available.
- OTP verification for educators is planned, providing a secure method for identity validation and account recovery—even offline.

---

## 3. Local Database

### Couchbase Lite

**How it works:**  
All user data, classes, klyps, and quiz results are stored locally using Couchbase Lite. The database is initialized on app launch, with schema migration and user context setup. Classes and klyps use document IDs (`class::{id}`, `klyp::{id}`) for efficient retrieval and management.

**Technical details:**  
- The repository layer abstracts all database operations, providing CRUD methods for classes and klyps.
- Error handling and fallback mechanisms ensure data integrity and user experience, even in failure scenarios.
- The database schema is designed for extensibility, supporting future features like cloud sync and richer content types.

---

## 4. Architecture

### Technology Stack

**How it works:**  
Klypt is developed in Kotlin, using Jetpack Compose for UI. The app is built on Google AI Edge stack, supporting multimodal LLMs (Gemma 3n, 2B, 4B, 8B) for on-device content generation and personalization. The architecture is modular, allowing for future expansion—voice input, image-based learning, and multilingual prompts are planned features.

**Technical details:**  
- The app uses a Model-View-ViewModel (MVVM) architecture, with StateFlow for reactive UI updates.
- Secure API key handling is implemented for model downloads and updates.
- The navigation graph is type-safe, with proper argument handling and URL encoding/decoding for class IDs.

---

## 5. Gamified Learning Model

### Gamification Components

**How it works:**  
Klypt tracks quiz completion, scores, streaks, and mastery metrics. Students earn rewards and progress through adaptive difficulty levels, making learning interactive and motivating. Educators can create and edit quizzes with rich content, leveraging AI for question generation.

**Technical details:**  
- The quiz editor allows educators to add, edit, and delete questions, with support for generated options.
- Progress tracking is implemented in the local database, with achievements and streaks displayed in the UI.
- Planned features include adaptive difficulty and content mastery, further enhancing engagement and personalizing the learning journey.

---

## 6. Class & Klyp Management

### Class Details

**How it works:**  
The "Class Details" screen displays class information (title, code, student count, klyp count) and lists all klyps for the class. Users can add new klyps via a dialog, delete klyps with confirmation, and view empty/error/loading states.

**Technical details:**  
- The screen is implemented using Jetpack Compose, with consistent styling and confirmation dialogs for destructive actions.
- State management is handled by `ClassDetailsViewModel`, which supports initialization with either a `ClassDocument` or class ID.
- Navigation is type-safe, with proper argument handling and back navigation.

### ViewModels & Repositories

**How it works:**  
`ViewAllClassesViewModel` and `ClassDetailsViewModel` manage UI state, handle CRUD operations, and integrate with the repository layer. Repositories provide methods for saving, retrieving, and deleting classes and klyps, with proper document ID management.

---

## 7. Export/Import Enhancements

Exported/imported files follow a strict JSON schema for compatibility and transparency. Enhanced error handling and user feedback ensure that users are informed of any issues during export/import. The share dialog provides clear options for exporting/importing classes and klyps.

---

## 8. Challenges & Technical Decisions

1. **Role Management:** Students and educators follow distinct data schemas, requiring careful handling across all logic layers, especially when performing shared operations such as importing or modifying class data.
2. **Model Context Handling:** Since models operate on isolated prompts, we had to implement a mechanism to pass relevant data before invoking model operations—ensuring the AI responds with appropriate context.
3. **Voice Input Limitations:** Gemma 3n’s voice input is capped at around 30 seconds. To work around this, we opted to integrate external Text-to-Speech (TTS) tools, allowing users to transcribe voice inputs into text manually for consistent performance.
4. **Class Data Synchronization During Import:** When importing a class, all existing klyps tied to the same class ID are overwritten. To preserve data integrity, we added logic to prevent users who are not the original authors from modifying or appending to that class, keeping contributions isolated.
5. **App Bundling and Distribution:** Packaging an AI-powered app with large models is non-trivial. We drew inspiration from the Google AI Edge Gallery and implemented model packaging via Hugging Face access tokens, allowing us to keep the app lightweight while downloading models on demand.
6. **Class Creation Workflow:** Initially, the app auto-generated classes when class context wasn’t passed between screens, leading to unexpected class duplication and data loss—especially after switching models in the LLM Chat interface. We resolved this by requiring users to explicitly select or create a class before proceeding, making the workflow predictable and robust.

---

## 9. Bug Reporting & Contribution

Users can report bugs via GitHub Issues, following a provided template. Contribution guidelines ensure code quality and consistency, with a clear review process for new features and bug fixes.

---

## 10. Usage Flow

1. **Educator generates klyps then creates a class** and exports it as a `.json` file.
2. **Students import the file or generate klyps** and access quizzes instantly, offline.
3. **Login and data management** are handled locally, with future sync support.
4. **Class and klyp management** via dedicated screens and dialogs.
5. **Progress and achievements** tracked and displayed, motivating continued engagement.

---

## 11. A Real Problem, A Real Solution

One of our team members, Ahmed, tested the app with his younger siblings in a household with unreliable internet access. Using only a shared class file and the Klypt app, they were able to access quizzes and begin learning—completely offline and instantly. This real-world use case illustrates our core mission: delivering powerful, AI-driven educational experiences to underconnected communities around the world.

---

## 12. Aligned with Global Goals

Klypt aligns with the United Nations Sustainable Development Goal 4: Quality Education, which seeks to "ensure inclusive and equitable quality education and promote lifelong learning opportunities for all." By enabling accessible, offline-first learning powered by AI, Klypt contributes directly to this global mission.

---

## 13. Future Enhancements

- View resources in klyp details screen, bulk operations, search/filter, richer content, online sync, student management, and more.

---

## 14. Testing Recommendations

- Test class/klyp creation, deletion, navigation, empty/error states, user roles, offline scenarios, and large datasets.

---

Klypt is a robust, extensible platform for offline, AI-powered education, built with transparency, modularity, and user empowerment at its core. Every feature is engineered for reliability, extensibility, and user empowerment, with detailed documentation and implementation guides supporting ongoing development and improvement.
