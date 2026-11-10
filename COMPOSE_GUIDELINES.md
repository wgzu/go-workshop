# Compose Multiplatform — Project Starter Guidelines

> **Purpose:** A practical engineering baseline for new Compose Multiplatform (KMP) applications. Use this document during project setup, feature implementation, code review, and AI-assisted development.
>
> **Scope:** Android + iOS first. Desktop and Web may be added when needed. A Ktor server is optional and is **not** part of the client app's dependency graph.
>
> **Architecture inspiration:** Ahmed Nassar's [Fully Modular Kotlin Multiplatform Architecture — Part 1](https://medium.com/@inassar0/building-a-fully-modular-kotlin-multiplatform-architecture-for-android-ios-web-desktop-server-5933ed62c0a3), [Part 2](https://medium.com/@inassar0/inside-the-feed-module-a-complete-kotlin-multiplatform-deep-dive-with-diagrams-code-824613457207), and the accompanying [reference repository](https://github.com/inassar0/kmp_instagram_clone). This is an **adapted starter guideline**, not a literal copy of the reference implementation.

## 1. Goals and non-goals

### Goals

- Share business logic and Compose UI where the platform supports it.
- Organize code by **feature**, with clear domain, data, and presentation boundaries.
- Make features independently understandable, testable, and removable.
- Keep platform entrypoints and the application composition root thin.
- Prefer unidirectional data flow, type-safe navigation, and explicit dependencies.
- Start small, but allow the project to grow without a structural rewrite.

### Non-goals for initial setup

- Creating a microservice backend merely because the reference project has one.
- Supporting Android, iOS, Web, and Desktop on day one without a product requirement.
- Splitting every model, mapper, or use case into its own Gradle module.
- Implementing offline-first synchronization, a generic MVI framework, or a custom networking SDK before the first feature needs them.

## 2. Core architectural rules

1. **Feature-first structure:** Organize business capabilities under `feature/<name>` rather than global `screens/`, `viewmodels/`, and `repositories/` folders.
2. **No feature-to-feature dependencies:** `feature:home` must not import `feature:profile` directly. Coordinate through navigation contracts, shared interfaces, or the app composition root.
3. **Core never depends on features:** `core:*` can be imported by features; the reverse is forbidden.
4. **Domain is framework-agnostic:** Business models, repository contracts, and use cases must not depend on Compose, Ktor, SQLDelight, or Android/iOS APIs.
5. **Data implements domain contracts:** Concrete repositories coordinate API and persistence. ViewModels do not access raw HTTP clients, SQL queries, or database drivers.
6. **Presentation owns UI state:** Screens render immutable state and emit actions. ViewModels orchestrate use cases, loading/error states, and screen effects.
7. **Platform APIs stay at the edges:** Prefer portable APIs; use `expect/actual`, interfaces, or platform-specific implementations only where necessary.
8. **Prefer constructor injection:** Koin wires dependencies at the composition root. Avoid service locators in domain logic and hidden global singletons.
9. **Dependencies are explicit and minimal:** Use `implementation` unless another module genuinely requires a public API exposure.
10. **Test boundaries, not implementation details:** Most tests should target domain rules, repository behavior, and state transitions.

### Dependency direction

```text
Android / iOS / Desktop / Web entrypoints
                    |
                    v
           composeApp (app wiring)
         /       |       \
        v        v        v
  feature:home  feature:auth  feature:settings
         |        |         |
         +--------+---------+
                  |
             core modules
       (ui, navigation, network,
           database, common)

Inside a feature:

 presentation --> domain <-- data
       |                     |
       |                     +--> remote / local data sources
       +--> renders UI state

Only the application composition root assembles concrete dependencies.
```

**Important distinction:** Calls flow from UI → ViewModel → use case/repository → data source, while **source-code dependencies** point from presentation/data toward domain contracts. A domain interface must not import a concrete repository implementation.

## 3. Recommended project layout

Use the currently generated KMP project from the official wizard as the build-system baseline; platform entrypoint folders may differ by template version. The organization below follows the article's feature-first approach.

```text
project-root/
├── composeApp/                    # Shared App(), navigation host, DI assembly
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       ├── jvmMain/               # Add when Desktop is needed
│       └── wasmJsMain/            # Add when Web is needed
├── iosApp/                        # Thin Xcode host (when applicable)
├── core/
│   ├── designsystem/              # Theme, tokens, reusable UI primitives
│   ├── navigation/                # Route and feature-entry contracts
│   ├── network/                   # Ktor client configuration, errors
│   └── database/                  # SQLDelight setup, drivers, migrations
├── shared/
│   └── platform/                  # Platform services, dispatchers, logging
├── feature/
│   ├── home/
│   │   └── src/commonMain/kotlin/.../home/
│   │       ├── domain/
│   │       │   ├── model/
│   │       │   ├── repository/
│   │       │   └── usecase/
│   │       ├── data/
│   │       │   ├── remote/
│   │       │   ├── local/
│   │       │   ├── mapper/
│   │       │   └── HomeRepositoryImpl.kt
│   │       ├── presentation/
│   │       │   ├── HomeScreen.kt
│   │       │   ├── HomeViewModel.kt
│   │       │   ├── HomeUiState.kt
│   │       │   └── HomeAction.kt
│   │       ├── navigation/HomeDestination.kt
│   │       └── di/HomeModule.kt
│   └── settings/                  # Same internal pattern when needed
├── gradle/libs.versions.toml      # Central dependency/version catalog
├── build-logic/                   # OPTIONAL: convention plugins later
├── settings.gradle.kts
└── README.md
```

**Starter recommendation:** Use **one Gradle module per feature**, with `data`, `domain`, and `presentation` as packages. This provides useful feature isolation without excessive module overhead. If a feature becomes large, split it later into `:feature:home:domain`, `:feature:home:data`, and `:feature:home:presentation` to enforce the internal dependency rules at compile time.

## 4. Responsibilities and allowed dependencies

| Module / package | Owns | Can depend on | Must not depend on |
|---|---|---|---|
| `composeApp` | `App()`, navigation host, startup DI, composition of feature entries | All feature entrypoints and necessary core modules | Business logic implementations in app root |
| `core:designsystem` | Theme, typography, colors, reusable UI | Compose, common resource APIs | Any feature |
| `core:navigation` | Type-safe navigation contracts, shared navigation abstractions | Multiplatform navigation APIs | Any feature implementation |
| `core:network` | Ktor setup, timeouts, serializers, HTTP/error mapping | Ktor and platform engines | Feature-specific API endpoints |
| `core:database` | SQLDelight configuration, driver factories, migrations | SQLDelight, platform drivers | Feature UI / ViewModels |
| `shared:platform` | Portable platform abstractions and implementations | Minimal platform APIs | Feature-specific behavior |
| `feature:x/domain` | Business entities, repository interfaces, use cases | Kotlin, coroutines/Flow if needed | Compose, Ktor, SQLDelight, platform SDKs |
| `feature:x/data` | DTOs, data sources, repository implementations, mappers | Domain + relevant infrastructure | UI, Compose screens, ViewModels |
| `feature:x/presentation` | Compose screens, state, ViewModels, UI mappers | Domain + design system/navigation | Concrete HTTP clients and SQL drivers |

Keep feature-specific caches and endpoints within the feature `data` package. Core supplies reusable infrastructure, not all features' business models.

## 5. Preferred technical stack

| Concern | Default | Notes |
|---|---|---|
| UI | Compose Multiplatform + Material 3 | Shared composables in `commonMain` |
| State | Kotlin Coroutines, `StateFlow`, ViewModel | One UI state stream per screen |
| DI | Koin | Register feature modules at startup |
| Navigation | Type-safe Compose Multiplatform navigation | Choose one supported navigation API for the project; do not mix stacks |
| HTTP | Ktor Client + kotlinx.serialization | Engines configured per platform |
| Local database | SQLDelight **when storage is needed** | Avoid adding a database prematurely |
| Async | Kotlin Coroutines / Flow | Structured concurrency |
| Tests | `kotlin.test`, coroutine test utilities | Test domain, repository, and ViewModel |
| Build | Gradle Kotlin DSL + version catalog | Align Kotlin, Compose, AGP, KSP, and other plugin versions |
| Formatting | ktlint or detekt/formatter chosen by team | Enforce a single style in CI |

**Version policy:** Use versions compatible with the **current** KMP wizard and official compatibility tables. Do not copy old versions blindly from the article or repository. Add Desktop/Web only after confirming target/library compatibility.

## 6. Feature implementation standard

Implement each feature as a vertical slice:

1. Define domain models and a minimal repository contract.
2. Implement the repository and only the data sources it needs.
3. Expose immutable screen state and explicit UI actions.
4. Implement a ViewModel that transforms actions into state updates and effects.
5. Build stateless, previewable Compose content.
6. Add a typed route and feature-entry registration.
7. Provide a Koin module and wire it in `composeApp`.
8. Add tests for a success path, an error path, and important state transitions.

### 6.1 Domain contract example

```kotlin
// feature/home/domain/model/HomeItem.kt
data class HomeItem(val id: String, val title: String)

// feature/home/domain/repository/HomeRepository.kt
interface HomeRepository {
    suspend fun getItems(): List<HomeItem>
}
```

Domain types must not expose DTOs, SQL rows, `HttpResponse`, or Compose-specific state.

### 6.2 State and actions

```kotlin
// feature/home/presentation/HomeUiState.kt
data class HomeUiState(
    val isLoading: Boolean = false,
    val items: List<HomeItemUi> = emptyList(),
    val errorMessage: String? = null,
)

data class HomeItemUi(val id: String, val title: String)

sealed interface HomeAction {
    data object Refresh : HomeAction
    data class ItemClicked(val id: String) : HomeAction
}
```

- Expose `StateFlow<HomeUiState>` read-only from the ViewModel.
- Use `viewModelScope` for screen-owned asynchronous work.
- Keep mutations inside the ViewModel (`MutableStateFlow.update`).
- Treat navigation, snackbars, and permissions as deliberate effects/callbacks, not data-fetching logic in a composable body.
- Don't introduce a generic `BaseViewModel` or universal reducer until duplication clearly justifies it.

### 6.3 Stateless Compose rendering

```kotlin
@Composable
fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Render state only; invoke onAction from user interaction.
    // No repository, HTTP, or database calls here.
}
```

A small route-level composable can obtain the ViewModel, collect state with lifecycle awareness, and call `HomeContent`. Keep previews focused on `HomeContent` with sample states. Use strings and images from multiplatform resources rather than hardcoded user-facing copy.

## 7. Navigation and app scaffold

### Type-safe destinations

```kotlin
// Declared in feature/home/navigation/HomeDestination.kt
@Serializable
data object HomeDestination

@Serializable
data class DetailsDestination(val itemId: String)
```

- Use typed destinations, not string concatenation for routes.
- Pass identifiers between screens; retrieve detailed data inside the destination's ViewModel.
- Own navigation graph registration in each feature; assemble feature entries in `composeApp`.
- Keep navigation *requests* distinct from UI rendering so screens remain testable.
- **Kotlin caveat:** Do **not** put a `sealed interface AppRoute` in one Gradle module and implement it in other feature Gradle modules. Direct inheritors of a sealed type must obey Kotlin's package/module restrictions. Use independently serializable route types or a non-sealed shared interface if a common contract is necessary.

### Shared scaffold

- Keep a single app-level `Scaffold` for common top bar, bottom navigation, insets, and theme.
- Let a destination provide a small `ScreenUiConfig` describing title and visible chrome where necessary.
- Do not duplicate global app bars or bottom bars in every feature.
- Keep screen-specific actions in the feature, using callbacks or configuration passed to the app host.

## 8. Networking and persistence

### Networking: `core:network`

- Configure Ktor once with `ContentNegotiation`, JSON serialization, timeout policy, and environment-based base URL.
- Supply an appropriate HTTP engine for each target; platform-specific dependencies belong in their source sets.
- Put feature endpoints (for example `HomeRemoteDataSource`) inside feature data packages.
- Map transport errors to meaningful domain/data errors at a well-defined boundary.
- Never log access tokens, user credentials, or sensitive payloads.
- Apply retries only to suitable retryable/idempotent operations; honor backoff and cancellation.
- Let cancellation propagate: do **not** turn `CancellationException` into a normal failure result.

### Persistence: `core:database`

- Introduce SQLDelight only when offline support, caching, or structured local persistence is required.
- Keep schema setup and driver creation in `core:database`; keep feature-specific queries/adapters in feature data where practical.
- Use versioned migrations, test migration paths, and define cache invalidation rules explicitly.
- Make the repository the single decision point for remote/cache precedence and refresh behavior.
- Do not promise offline-first behavior unless the cache and synchronization strategy actually supports it.

## 9. Dependency injection

Feature modules register their own dependencies. The app starts Koin **once** and combines infrastructure + feature modules.

```kotlin
// feature/home/di/HomeModule.kt
val homeModule = module {
    single<HomeRepository> { HomeRepositoryImpl(remote = get()) }
    single<HomeRemoteDataSource> { KtorHomeRemoteDataSource(client = get()) }
    viewModel { HomeViewModel(repository = get()) }
}

// App composition root: conceptual wiring
val appModules = listOf(
    platformModule,
    networkModule,
    homeModule,
)
```

The example assumes compatible Koin ViewModel integration and the named classes have been implemented. It illustrates ownership/wiring rather than a complete drop-in app.

- Prefer constructor injection over calling `get()` deep inside business logic.
- Choose `single`, `factory`, or ViewModel scope according to lifecycle and ownership.
- Keep environment configuration injectable so tests can use a fake base URL or repository.
- Do not let a feature register or initialize another feature's dependencies.

## 10. Platform-specific functionality

- Put shared feature logic and UI in `commonMain` when possible.
- Put Android/iOS-specific wiring in their corresponding source sets.
- Prefer **interfaces plus platform DI implementations** when behavior is substantial; use `expect/actual` for narrow platform APIs when appropriate.
- Keep `Context`, Android Activity classes, UIKit types, Java-only classes, and platform-specific storage APIs out of domain and common UI code.
- Confirm third-party libraries support every **enabled** target before introducing them to `commonMain`.
- For the server, share only platform-neutral contracts/models where useful; do **not** reuse client Compose feature modules in the backend.

## 11. Compose UI and performance rules

- Prefer small composables with clear parameters and immutable screen models.
- Hoist state; avoid duplicating the same source of truth between UI and ViewModel.
- Use `remember` for local, transient Compose state; `rememberSaveable` for suitable UI restoration; ViewModel/repository for longer-lived state.
- Use stable keys in lazy lists and paginate large datasets instead of loading everything into one list.
- Avoid network/database work during composition and avoid expensive work on the main thread.
- Do not overuse `derivedStateOf`, `remember`, or custom stability annotations without evidence of a performance issue.
- Respect safe areas, window insets, text scaling, accessibility labels, and touch target sizes.
- Design layouts to adapt to compact/wide screens rather than relying only on device type.
- Validate launch time, scrolling, and memory use on lower-end Android devices as well as iOS.

## 12. Testing, quality, and CI

### Minimum test coverage for each feature

- **Domain:** business rules and use cases with fake repository contracts.
- **Repository:** success/error mapping, cache/remote precedence, retry/cancellation behavior where applicable.
- **ViewModel:** action → loading → success/error state transitions using a controlled coroutine test dispatcher.
- **UI:** important rendering and interactions on at least one supported UI test target; expand platform coverage for platform-specific behavior.
- **Database:** migration tests if SQLDelight is present.

### Pull request acceptance checklist

- [ ] Feature has no direct dependency on another feature.
- [ ] Domain does not import presentation or infrastructure libraries.
- [ ] Screen content takes state + callbacks and can be previewed/tested.
- [ ] Loading, empty, success, and error states are addressed where relevant.
- [ ] Network and database access occur through data/repository abstractions.
- [ ] No secrets, tokens, or environment credentials are committed.
- [ ] New libraries are compatible with all enabled target platforms.
- [ ] Formatting, compilation, and appropriate tests pass.
- [ ] Accessibility and basic small-screen behavior are checked.
- [ ] Feature README or architectural notes are updated when boundaries change.

### CI baseline

1. Validate Gradle configuration and dependency resolution.
2. Run formatting/lint checks.
3. Compile enabled targets.
4. Run common/unit tests; run UI/platform tests where runners are available.
5. Build platform artifacts only in appropriate pipelines.

Use `./gradlew tasks` to inspect actual generated task names; Gradle task names and target support differ by wizard/plugin version and enabled platforms.

## 13. Implementation order for a new project

### Phase 1 — Bootstrap (must-have)

- [ ] Generate KMP + Compose project with Android and iOS targets.
- [ ] Confirm Android and iOS sample app launches.
- [ ] Configure version catalog, package namespace, app environments, and README.
- [ ] Create `core:designsystem` and minimal theme/tokens.
- [ ] Set up one navigation host with typed destinations.
- [ ] Set up basic Koin bootstrap and a fake/in-memory repository.

### Phase 2 — One complete vertical slice (must-have)

- [ ] Add `feature:home` with domain/data/presentation packages.
- [ ] Render loading, success, empty, and error states.
- [ ] Connect screen → ViewModel → repository → fake data source.
- [ ] Wire typed navigation and the app scaffold.
- [ ] Add ViewModel + repository unit tests.

### Phase 3 — Real infrastructure (only as required)

- [ ] Add `core:network` and a real Ktor implementation.
- [ ] Add auth/session support if required by product scope.
- [ ] Add SQLDelight cache **only** if there is a persistence use case.
- [ ] Add platform services for permissions, files, camera, or notifications as needed.
- [ ] Add CI checks and platform build jobs.

### Phase 4 — Scale when justified

- [ ] Add new features without feature-to-feature imports.
- [ ] Extract common UI only after repeated use across features.
- [ ] Add convention plugins when Gradle configuration duplication becomes costly.
- [ ] Add Desktop/Web after platform validation and design adaptation.
- [ ] Split individual features into multiple Gradle modules only where boundary enforcement or build scale needs it.

## 14. Implementation instructions for developers and AI coding agents

When implementing a task in this repository:

1. **Read this file and inspect the existing module/source-set layout first.** Preserve established module naming and build conventions.
2. **Change only the modules needed for the task.** Avoid unrelated refactors and speculative abstraction layers.
3. **Implement a complete vertical slice**, not an unconnected UI mock: domain contract → data source/repository → state/ViewModel → Compose UI → navigation/DI → tests (skip layers the feature genuinely does not need).
4. **Follow dependency boundaries strictly.** Never import one feature's implementation from another feature. Never move business logic into `composeApp` or a composable body.
5. **Prefer simple, readable Kotlin** over generic frameworks and premature indirection. Use descriptive types, explicit error handling, and structured concurrency.
6. **Do not invent API responses, DB schemas, UI requirements, or navigation behavior.** Use provided specifications; otherwise use clearly labeled fakes/TODOs that cannot be mistaken for production functionality.
7. **Verify builds and tests for the affected enabled targets.** Report any target not run (for example, iOS unavailable without macOS).
8. **In the final implementation summary**, state files/modules changed, major architectural decisions, tests/builds run, and known limitations.

## 15. Anti-patterns to reject

- `feature:a` imports screens, ViewModels, or repositories from `feature:b`.
- `core` holds feature-specific endpoints, screen state, or business rules.
- A composable performs HTTP requests, database reads, or complex business decisions directly.
- ViewModels depend directly on `HttpClient`, SQLDelight generated queries, or platform SDK classes.
- Repository interfaces are defined inside data implementations instead of domain contracts.
- UI uses raw DTOs/database entities as public state.
- Navigation relies on ad-hoc route strings and serialized objects larger than necessary.
- Feature Gradle modules exist only to mirror every folder, creating avoidable build overhead.
- Global mutable singletons replace DI and make unit tests dependent on startup order.
- `runCatching` or broad exception handling silently swallows coroutine cancellation.
- A shared `server` module leaks server dependencies into client feature modules.

## References and maintenance

- [Ahmed Nassar — Part 1: Fully Modular KMP Architecture](https://medium.com/@inassar0/building-a-fully-modular-kotlin-multiplatform-architecture-for-android-ios-web-desktop-server-5933ed62c0a3)
- [Ahmed Nassar — Part 2: Inside the Feed Module](https://medium.com/@inassar0/inside-the-feed-module-a-complete-kotlin-multiplatform-deep-dive-with-diagrams-code-824613457207)
- [Reference implementation: `inassar0/kmp_instagram_clone`](https://github.com/inassar0/kmp_instagram_clone)
- [Official Kotlin Multiplatform quickstart](https://kotlinlang.org/docs/multiplatform/quickstart.html)
- [JetBrains: New default KMP project structure (2026)](https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/)

**Maintenance rule:** Revisit dependency versions, navigation API choice, and source-set configuration when upgrading Kotlin/Compose/AGP. Architecture principles are intended to remain stable; exact plugin configuration is version-dependent.
