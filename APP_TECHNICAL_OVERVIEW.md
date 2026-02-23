# APP_TECHNICAL_OVERVIEW.md

## 1. App Overview

### What the application does
MoneyPilot is a comprehensive, offline-first personal finance management application for Android. It allows users to track their income and expenses, manage multiple accounts (wallets), create budgets, set financial goals, and view detailed analytics of their spending habits. The application is designed for granular control, with features for categorizing transactions, handling transfers between accounts, and providing daily summaries.

### Primary use case
The primary use case is for a user to meticulously log every financial transaction they make—be it income, an expense, a contribution to a savings goal, or a loan repayment. By consistently logging this data, the user can gain a clear and detailed understanding of their financial health, see where their money is going, and make informed decisions based on visual reports and budget tracking.

### Target users (inferred from code)
The code suggests the target user is a detail-oriented individual who wants precise control over their financial data. The architecture caters to users who value:
-   **Data Privacy:** The offline-first approach with no apparent networking for core features implies a user who is cautious about sharing their financial data with third-party cloud services.
-   **Granular Tracking:** The presence of subcategories, transaction notes, and specific transaction types like "LOAN_REPAYMENT" and "GOAL_CONTRIBUTION" points to a user who wants to track more than just simple income and expenses.
-   **Customization:** The ability to manage categories, accounts, and set custom budget periods appeals to users who want to tailor the app to their specific financial situation.

### Core value proposition
MoneyPilot's core value proposition is providing a powerful, private, and highly detailed financial ledger on the user's device. It positions itself as a "data-first" tool, empowering users with complete ownership and control over their financial information, free from cloud-based subscriptions or privacy concerns.

## 2. Feature Breakdown

### 1. Transaction Management
-   **Description:** The core of the app. Users can add, edit, and delete transactions. Transactions are highly structured, requiring a type (Expense, Income, etc.), amount, description, date, time, and associated account.
-   **Key Files:**
    -   `TransactionEntity.kt`: The Room database model for a transaction.
    -   `TransactionDao.kt`: Provides database access methods for creating, reading, updating, and deleting transactions.
    -   `AddEditTransactionScreen.kt`: The main Composable UI for creating and editing transactions.
    -   `AddEditTransactionViewModel.kt`: The ViewModel that holds the state and business logic for the transaction entry screen.
    -   `DailyScreen.kt` & `DailyViewModel.kt`: UI for displaying a list of transactions for a given day.

### 2. Account (Wallet) Management
-   **Description:** Users can create and manage various financial accounts, which are referred to as "Wallets." Each account has a name, type (e.g., Bank, Cash, Credit Card), balance, and color/icon for identification. One account can be marked as primary.
-   **Key Files:**
    -   `AccountEntity.kt`: The Room database model for an account.
    -   `AccountDao.kt`: Database access for accounts.
    -   `WalletsViewModel.kt` & `WalletsScreen.kt`: UI for viewing, adding, and editing accounts.

### 3. Budgeting
-   **Description:** Allows users to set monthly or custom-period budgets for specific expense categories. The app tracks the amount spent against the budget and can provide alerts.
-   **Key Files:**
    -   `BudgetEntity.kt`: The Room model for a budget.
    -   `BudgetDao.kt`: Database access for budgets.
    -   `AddEditBudgetScreen.kt` & `AddEditBudgetViewModel.kt`: The UI and logic for creating or editing a budget.
    -   `BudgetsScreen.kt` & `BudgetsViewModel.kt`: The UI for displaying a list of all active budgets and their progress.

### 4. Savings Goals
-   **Description:** Users can define savings goals with a target amount and date. The app allows them to log contributions towards these goals, tracking their progress over time.
-   **Key Files:**
    -   `GoalEntity.kt`: The Room model for a savings goal.
    -   `GoalDao.kt`: Database access for goals.
    -   `AddEditGoalScreen.kt` & `AddEditGoalViewModel.kt`: UI and logic for creating/editing goals.
    -   `GoalsScreen.kt`: UI for displaying all goals.

### 5. Category Management
-   **Description:** Provides a hierarchical system for classifying transactions. Users can create parent categories (e.g., "Food") and nested subcategories (e.g., "Groceries," "Restaurants"). The app comes with a set of default categories that are seeded on first launch.
-   **Key Files:**
    -   `CategoryEntity.kt`, `SubcategoryEntity.kt`: Room models for categories and subcategories.
    -   `CategoryDao.kt`: Database access for both categories and subcategories.
    -   `CategoryManagerViewModel.kt` & `CategoryManagerScreen.kt`: UI for viewing, adding, and editing categories.

### 6. Daily Summary & Analytics
-   **Description:** The application provides a "Dashboard" with multiple views of the user's financial data, including a daily list of transactions, a monthly calendar view, and high-level reports. A background worker also generates a daily summary notification.
-   **Key Files:**
    -   `DailyScreen.kt`, `CalendarScreen.kt`, `ReportsScreen.kt`: The primary UI screens for these features.
    -   `DailyViewModel.kt`, `CalendarViewModel.kt`, `ReportsViewModel.kt`: The respective ViewModels.
    -   `DailySummaryWorker.kt`: A `WorkManager` worker that runs periodically to create a daily summary notification.

## 3. User Flow

The main user journey is centered around the `MainActivity`, which hosts a `Scaffold` with a `BottomAppBar` for primary navigation and a `NavHost` for the screen content.

1.  **Entry Point:** The user launches the app and lands on the **Dashboard**. `MainActivity.kt` is the entry point, which sets up the main navigation structure in `MoneyPilotNavHost.kt`.
2.  **Main Navigation (Bottom Bar):**
    -   **Dashboard:** The default screen (`DashboardScreen.kt`). This screen itself contains a top tab bar for navigating between `Daily`, `Calendar`, `Monthly`, and `Total` views.
    -   **Analytics:** Navigates to `ReportsScreen.kt` for visual charts.
    -   **Records:** Navigates to a searchable list of all transactions (`AllTransactionsScreen.kt`).
3.  **Adding a Transaction:**
    -   From the `DailyScreen`, the user clicks a Floating Action Button.
    -   This navigates them to the `AddEditTransactionScreen`.
    -   Here, they select a transaction type (Expense/Income), choose an account, category, and subcategory, enter an amount and description, and set the date/time.
    -   Saving the transaction commits it to the database via the `AddEditTransactionViewModel` and returns the user to the previous screen.
4.  **Managing Wallets/Budgets/Goals (Navigation Drawer):**
    -   The main `Scaffold` includes a navigation drawer.
    -   From the drawer, the user can navigate to dedicated screens for managing "Wallets" (`WalletsScreen.kt`), "Budgets" (`BudgetsScreen.kt`), "Goals" (`GoalsScreen.kt`), and "Categories" (`CategoryManagerScreen.kt`).
    -   Each of these screens provides a list view and allows the user to navigate to a corresponding "Add/Edit" screen.

## 4. Architecture & Tech Stack

-   **Architecture Pattern:** The application follows a modern **MVVM (Model-View-ViewModel)** architecture with strong influences from **Clean Architecture**.
    -   **UI Layer (View):** Composable functions (`...Screen.kt`) built with Jetpack Compose.
    -   **ViewModel Layer:** AndroidX `ViewModel` classes (`...ViewModel.kt`) that expose state via `StateFlow` and handle UI events.
    -   **Domain/Repository Layer (Model):** `...Repository.kt` interfaces and their `...RepositoryImpl.kt` implementations act as a Single Source of Truth, abstracting the data sources. The `domain` package, though present, seems underutilized, with most business logic residing in the ViewModels or Repositories directly.
-   **Key Frameworks and Libraries:**
    -   **UI:** Jetpack Compose (`androidx.compose`) for the entire UI layer. Material 3 (`androidx.compose.material3`) is used for components.
    -   **Navigation:** Jetpack Navigation for Compose (`androidx.navigation:navigation-compose`).
    -   **Asynchronous Programming:** Kotlin Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-core`) and Flow are used extensively for database operations and state management.
    -   **Data Storage:** Room (`androidx.room`) for the primary SQL database and Jetpack DataStore (`androidx.datastore:datastore-preferences`) likely for simple key-value preferences.
    -   **Dependency Injection:** Hilt (`com.google.dagger:hilt-android`) is used to manage dependencies throughout the app.
    -   **Background Processing:** WorkManager (`androidx.work:work-runtime-ktx`) for the `DailySummaryWorker`.
    -   **App Widgets:** Glance (`androidx.glance:glance-appwidget`) for home screen widgets.
-   **Networking Approach:** There is **no networking code** present for the core features. The application is designed to be **offline-first**.
-   **Data Storage Method:** A relational SQLite database managed by the Room Persistence Library.
-   **Dependency Injection:** Hilt is used for DI, with modules defined in the `di` package to provide instances of the database, DAOs, and repositories.

## 5. Module / Package Structure

The project is a single-module application (`:app`) with a well-organized package structure that separates concerns.

-   **`com.yourname.moneypilot`**: The root package.
    -   **`di`**: Contains Hilt modules (`DatabaseModule.kt`, `RepositoryModule.kt`) responsible for providing dependencies like the database instance, DAOs, and repository implementations.
    -   **`data`**: The data layer.
        -   **`local`**: Contains all on-device data sources.
            -   **`database`**: Defines the Room database (`MoneyPilotDatabase.kt`), Data Access Objects (`...Dao.kt`), entities (`...Entity.kt`), and type converters.
            -   **`preferences`**: (Inferred) Likely contains a `DataStore` implementation for user settings.
        -   **`repository`**: Contains repository implementations (`...RepositoryImpl.kt`) that provide a clean API for data access to the rest of the app.
    -   **`domain`**: Intended for business logic (e.g., Use Cases), but this layer appears to be underutilized in the current implementation.
    -   **`ui`**: The presentation layer.
        -   **`features`**: Contains sub-packages for each feature screen (e.g., `transactions`, `budgets`, `wallets`), each holding the corresponding Composable `Screen` and `ViewModel`.
        -   **`components`**: Reusable Jetpack Compose UI components used across multiple screens (e.g., `CalculatorKeyboard.kt`).
        -   **`theme`**: Standard Compose theme setup.
    -   **`util`**: Utility and helper classes, such as `SmsParser.kt`.
    -   **`widget`**: Contains the implementation for home screen widgets using Glance.
    -   **`worker`**: Contains `WorkManager` implementations, like `DailySummaryWorker.kt`.

## 6. Key Components

-   **`MainActivity.kt`**: The single Activity that serves as the entry point and host for the entire Jetpack Compose UI.
-   **ViewModels**: Every feature screen has a dedicated ViewModel (e.g., `AddEditTransactionViewModel`, `DailyViewModel`, `WalletsViewModel`) that manages its state and logic. The `AddEditTransactionViewModel` is particularly complex, handling the state for all transaction types and their associated data.
-   **`MoneyPilotDatabase.kt`**: The core Room database class that defines all the tables (entities) and provides access to the DAOs.
-   **`...RepositoryImpl.kt`**: These classes are the bridge between the data sources (DAOs) and the ViewModels, abstracting away the data implementation details.
-   **`DailySummaryWorker.kt`**: A `CoroutineWorker` scheduled to run periodically. Its purpose is to query the database for the day's transactions, calculate a summary, and display it as a system notification.

## 7. Current Development Status

-   **Production-ready vs Prototype Indicators:** The application appears to be in a **late-stage prototype or early beta** phase. The architecture is robust, the database schema is comprehensive, and many features are implemented. However, the presence of critical bugs, `TODO` comments, and some incomplete features indicates it is not yet production-ready.
-   **TODOs, commented code, placeholders:** The codebase contains several `TODO` markers, particularly in the `AddEditTransactionViewModel`, indicating areas that need further work. There is also commented-out code in several places, suggesting recent refactoring or debugging attempts.
-   **Potential Risks or Technical Debt:**
    -   **CRITICAL BUG:** The `AddEditTransactionScreen` is currently non-functional due to a severe bug preventing the display of Category/Subcategory fields. This is the highest priority risk as it blocks the app's core functionality. The root cause is a race condition in the `AddEditTransactionViewModel`'s data loading logic.
    -   **ViewModel Complexity:** The `AddEditTransactionViewModel` is a "god object" that handles the logic for all transaction types (Expense, Income, Goal, Loan). This makes it very complex, difficult to maintain, and prone to bugs like the one currently blocking development.
    -   **Limited Domain Layer:** Business logic is scattered between ViewModels and Repositories. A more clearly defined domain layer with specific Use Cases would improve separation of concerns and testability.

## 8. Improvement Suggestions

-   **Architecture Improvements:**
    -   **Refactor `AddEditTransactionViewModel`:** Break it down into smaller, more focused ViewModels or helper classes based on the transaction type. A `State` and `Event` sealed hierarchy for each type could manage its specific logic, which the main ViewModel would then delegate to.
    -   **Introduce Use Cases:** Move business logic out of ViewModels and Repositories into dedicated Use Case classes in the `domain` package (e.g., `SaveTransactionUseCase`, `GetDailySummaryUseCase`). This would make the ViewModels leaner and the business logic more reusable and testable.
-   **Performance Optimizations:**
    -   **Database Queries:** Review all Room DAO queries to ensure they are efficient and run on background threads. The use of `Flow` is good, but complex queries on large datasets could still be slow.
    -   **Startup Time:** The `categoryRepository.seedDefaults()` call in the `init` block of a ViewModel could potentially slow down app startup if the seeding process is complex. This should be a one-time operation managed by the database callback (`RoomDatabase.Callback`).
-   **Code Quality Recommendations:**
    -   **Fix the ViewModel Race Condition:** The data loading logic in `AddEditTransactionViewModel` must be rewritten to eliminate the race condition. Use `StateFlow` correctly or `combine` multiple flows to ensure data consistency before updating the UI state.
    -   **Null Safety:** Replace all unsafe not-null assertions (`!!`) with `requireNotNull()`, `checkNotNull()`, or safer handling of nullable types.
-   **Security Considerations:**
    -   **Database Encryption:** Since the app stores sensitive financial data, the Room database should be encrypted using a library like SQLCipher via `androidx.sqlite.db.SupportSQLiteOpenHelper.Factory`. This would protect user data if the device is compromised.

## 9. Missing Pieces

-   **`DistributionRuleEntity` Not Used:** The database contains an entity named `DistributionRuleEntity`, which suggests a feature for automatically distributing money between wallets. However, there is no corresponding DAO, Repository, ViewModel, or UI to manage or execute these rules. This feature is defined in the data model but appears to be completely unimplemented.
-   **Incomplete Features:** The `InvestmentEntity` and `BigBillEntity` are present, but their integration into the main user flow seems minimal compared to core transactions, budgets, and goals. There may be missing UI or logic for managing these.
-   **Edge Case Handling:** The `saveTransaction` logic has basic validation (e.g., amount > 0), but complex edge cases around editing transactions and the subsequent impact on historical budget or goal calculations might not be fully handled. For example, changing a transaction's date from one month to another could require recalculating two separate monthly budgets.
