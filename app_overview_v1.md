# MoneyPilot - Technical Overview & Architectural Analysis

## 1. App Overview

### What the application does
MoneyPilot is a personal finance management application designed for Android. It allows users to track their income and expenses, categorize transactions, and view financial reports. The application appears to be built for offline-first usage, with data stored locally on the device. It also includes features like data backup/restore and home screen widgets for quick financial snapshots.

### Primary use case
The primary use case is for an individual to meticulously log all financial transactions (both money in and money out) to gain a clear understanding of their spending habits and financial health over time.

### Target users (inferred from code)
The target users are individuals who prefer a hands-on approach to budget and expense tracking. The feature set suggests a user who is detail-oriented and wants to manage their finances without linking directly to bank accounts, prioritizing privacy and manual control.

### Core value proposition
The core value proposition of MoneyPilot is to provide a simple, private, and robust tool for manual financial tracking. Its offline-first architecture ensures that the app is always functional, and the inclusion of modern Android components like Glance widgets and Material 3 provides a contemporary user experience.

## 2. Feature Breakdown

Based on the project's dependencies and common Android patterns, the following features are inferred:

*   **Transaction Management**: Users can create, edit, and delete financial transactions. This is the central feature of the application.
    *   *Key Files (Inferred)*: `TransactionDao.kt`, `TransactionRepository.kt`, `AddEditTransactionViewModel.kt`, `AddEditTransactionScreen.kt`.

*   **Category Management**: Users can create and manage categories (e.g., "Groceries," "Salary," "Utilities") to assign to their transactions.
    *   *Key Files (Inferred)*: `CategoryDao.kt`, `CategoryRepository.kt`, `CategoriesViewModel.kt`, `CategoriesScreen.kt`.

*   **Financial Reporting**: The app likely generates reports, such as monthly summaries or spending by category. The presence of `ChevronLeft` and `ChevronRight` icons strongly suggests a date-based navigation UI for viewing these reports.
    *   *Key Files (Inferred)*: `ReportsViewModel.kt`, `ReportsScreen.kt`.

*   **Dashboard/Home Screen**: A central screen that likely displays a summary of recent activity, current balance, or quick stats.
    *   *Key Files (Inferred)*: `DashboardViewModel.kt`, `DashboardScreen.kt`.

*   **Data Persistence**: All user data (transactions, categories) is stored locally on the device.
    *   *Key Files (Inferred)*: `AppDatabase.kt`, `Transaction.kt` (Entity), `Category.kt` (Entity).

*   **Data Backup & Restore**: The application appears to support background tasks, which are commonly used for exporting or importing user data. The `hilt-work` dependency points to this.
    *   *Key Files (Inferred)*: `BackupWorker.kt`, `RestoreWorker.kt`, `SettingsViewModel.kt`.

*   **Home Screen Widgets**: Users can add widgets to their home screen for a "glanceable" view of their finances.
    *   *Key Files (Inferred)*: `FinancialSummaryWidget.kt`, `FinancialSummaryWidgetReceiver.kt` (using `androidx.glance`).

*   **User Settings**: A screen for managing application preferences, such as currency, theme, or data management options.
    *   *Key Files (Inferred)*: `SettingsScreen.kt`, `SettingsViewModel.kt`, `UserPreferencesRepository.kt` (using `androidx.datastore`).

## 3. User Flow

The main user journey can be described as follows:

1.  **Onboarding/First Launch**: The app opens to a primary dashboard or a list of recent transactions. If no data exists, it might show an empty state prompting the user to add their first transaction.
2.  **Adding a Transaction**: The user navigates (likely via a Floating Action Button) to an "Add Transaction" screen. Here, they input an amount, select a type (income/expense), choose a category, and save it.
3.  **Viewing Data**:
    *   The user returns to the **Dashboard** to see updated summaries.
    *   They can navigate to a **Reports Screen** to view historical data, using date selectors (e.g., `< March 2024 >`) to change the time period.
    *   A dedicated **Transactions List** screen likely allows them to view all entries.
4.  **Managing Categories**: From a settings or management area, the user can navigate to a **Categories Screen** to add, edit, or delete the categories used for tagging transactions.
5.  **Data Management**: Within the **Settings Screen**, the user can trigger a background job to back up their data to a file or restore from a previous backup.

The navigation is managed by **Jetpack Navigation for Compose**, allowing for a single-Activity architecture where different screens are composable destinations.

## 4. Architecture & Tech Stack

The project follows a modern, clean architecture pattern, likely a variation of **MVVM (Model-View-ViewModel)** combined with a repository layer.

*   **Architecture Pattern**: **MVVM with a Repository pattern**.
    *   **View**: Jetpack Compose screens (`*Screen.kt`).
    *   **ViewModel**: `androidx.lifecycle.ViewModel` classes (`*ViewModel.kt`) hold UI state and business logic.
    *   **Model**: Repositories (`*Repository.kt`) abstract data sources, and Room DAOs (`*Dao.kt`) interact with the database.

*   **Key Frameworks and Libraries**:
    *   **UI**: **Jetpack Compose** (`androidx.compose`) using the **Material 3** design system.
    *   **Architecture Components**:
        *   **ViewModel**: `androidx.lifecycle:lifecycle-viewmodel-compose`
        *   **Navigation**: `androidx.navigation:navigation-compose`
        *   **Lifecycle**: `androidx.lifecycle:lifecycle-runtime-compose`
    *   **Asynchronous Programming**: **Kotlin Coroutines** (`kotlinx-coroutines-core` and `android`) are used for background tasks and asynchronous data flow, likely with `Flow`.
    *   **Data Storage**:
        *   **Database**: **Room** (`androidx.room`) for structured SQL database storage.
        *   **Preferences**: **Jetpack DataStore** (`androidx.datastore-preferences`) for simple key-value settings.
    *   **Dependency Injection**: **Hilt** (`com.google.dagger:hilt-android`) is used to manage dependencies throughout the app, including injecting repositories into ViewModels and managing database instances.
    *   **Background Processing**: **WorkManager** (`androidx.work:work-runtime-ktx`), integrated with Hilt via `androidx.hilt:hilt-work`.
    *   **Widgets**: **Glance** (`androidx.glance`) for building modern home screen app widgets.
    *   **Logging**: **Timber** (`com.jakewharton.timber:timber`) for structured logging.

*   **Networking Approach**: There are no direct networking libraries (like Retrofit or Ktor) listed in the dependencies. This strongly implies the application is **offline-first** and does not communicate with a backend API for its core functionality.

*   **Data Storage Method**: A combination of Room for complex, relational data (transactions, categories) and DataStore for user preferences.

## 5. Module / Package Structure

The project appears to be a single-module application (`:app`), but the internal package structure likely follows clean architecture principles.

*   **`com.yourname.moneypilot.ui`**: Contains all Jetpack Compose UI code.
    *   `features`: Sub-packages for each distinct feature screen (e.g., `dashboard`, `reports`, `addedit`).
    *   `components`: Reusable UI components shared across multiple screens.
    *   `theme`: App theme, colors, and typography.
*   **`com.yourname.moneypilot.data`**: The data layer of the application.
    *   `local`: Contains Room database definitions (`AppDatabase`), DAOs, and entity classes.
    *   `repository`: Repository implementations that provide a clean API for data access to the ViewModels.
*   **`com.yourname.moneypilot.di`**: Hilt dependency injection modules.
*   **`com.yourname.moneypilot.domain`**: (Optional but likely) Contains business logic, use cases, or model classes that are independent of the Android framework.
*   **`com.yourname.moneypilot.worker`**: Contains `WorkManager` `Worker` implementations.
*   **`com.yourname.moneypilot.widget`**: Contains `GlanceAppWidget` implementations for home screen widgets.

## 6. Key Components

*   **ViewModels**: `ReportsViewModel`, `TransactionViewModel`, `CategoryViewModel`, etc. Each is responsible for preparing and managing data for a specific screen or feature.
*   **Room Database (`AppDatabase.kt`)**: The central point for database creation and DAO access. It defines the tables (entities) and versions for the app's data.
*   **Repositories (`*Repository.kt`)**: These classes are the single source of truth for the app's data. They abstract away whether the data comes from the local database or another source and are injected into ViewModels.
*   **Workers (`BackupWorker.kt`, etc.)**: These components handle deferrable, background tasks like data backup, ensuring they run even if the app is closed.
*   **Navigation Host (`NavHost`)**: The central composable that defines the navigation graph for the entire application, mapping routes to Composable screens.

## 7. Current Development Status

*   **Production-ready vs. Prototype**: The tech stack is modern and robust, suggesting this is a serious project, not a simple prototype. The use of Hilt, Room, WorkManager, and a structured architecture points towards a production-quality application.
*   **TODOs and Placeholders**: The error message (`Unresolved reference: ChevronLeft`) indicates the project is under active development. There are likely other areas with `// TODO` comments or placeholder logic that are not yet complete.
*   **Potential Risks or Technical Debt**:
    *   **Dependency Mismatches**: The original build error indicates a potential mismatch between the Compose BOM version and the version of a specific library (`material-icons-extended`) where a feature was expected. The project uses a BOM (`2024.06.00`) but also specifies individual library versions (`1.6.1`), which can lead to version conflicts. It's better to rely solely on the BOM for managed dependencies.
    *   **Missing Error Handling**: Without seeing the code, it's unclear how well edge cases (e.g., database errors, backup failures) are handled. This is a common area for technical debt.
    *   **Test Coverage**: The presence of an `androidTest` module is a good sign, but the extent and quality of test coverage are unknown.

## 8. Improvement Suggestions

*   **Architecture Improvements**:
    *   **Use Case Layer**: Introduce a `domain` layer with Use Cases (or Interactors) to encapsulate specific business logic. This decouples ViewModels from repositories and makes the business logic more reusable and testable.
    *   **BOM-Only Dependency Management**: Remove explicit versions for Compose libraries (like `ui`, `foundation`, `material3`) in `build.gradle.kts` and let the `compose-bom` manage them entirely to prevent version conflicts.

*   **Performance Optimizations**:
    *   **Database Queries**: Ensure all Room database queries are performed on a background dispatcher (`Dispatchers.IO`).
    *   **Lazy Composables**: Use `LazyColumn` and `LazyRow` for displaying lists of transactions or categories to ensure efficient composition.
    *   **ViewModel Scoping**: Ensure ViewModels are scoped correctly to the navigation graph (`hiltViewModel()`) to avoid data being re-fetched unnecessarily.

*   **Code Quality Recommendations**:
    *   **State Management**: For complex screens, consider using a formal State class (e.g., `data class ReportsUiState(...)`) managed by the ViewModel's `StateFlow` to ensure predictable state updates.
    *   **Static Analysis**: Integrate static analysis tools like `detekt` and `ktlint` to enforce code style and catch potential issues early.

*   **Security Considerations**:
    *   **Backup Security**: If the backup feature exports to a user-accessible location, the data is unencrypted. Consider offering an option for encrypted backups (e.g., password-protected zip).
    *   **Database Encryption**: For highly sensitive financial data, consider using `SQLCipher` with Room to encrypt the database at rest.

## 9. Missing Pieces

*   **Cloud Sync**: The application appears to be fully offline. A key missing feature for many users would be automatic cloud synchronization and backup across multiple devices.
*   **Bank Account Integration**: The manual-entry approach is deliberate, but a major feature common in other finance apps is linking bank accounts via an API like Plaid. This is intentionally out of scope based on the current architecture.
*   **Advanced Reporting**: The reporting seems to be based on simple date navigation. More advanced features like custom date ranges, chart visualizations (e.g., pie charts for category spending), and trend analysis appear to be missing.
*   **Comprehensive Error Handling**: It's inferred that full user-facing error handling for background tasks (like a "Backup failed" notification) might not be fully implemented yet.
