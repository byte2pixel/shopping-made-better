# Cookbook: Adding a Use-Case-Driven Feature

This guide shows how to add a feature that reads (or writes) data through Supabase
using the app's **use-case-driven** architecture. It uses the existing **stores**
feature as a worked example — every step links to a real file you can open and copy.

Links:
- [Android Developers - Modern app architecture](https://developer.android.com/topic/architecture#modern-app-architecture)

## The pattern at a glance

```
Compose Screen  →  ViewModel  →  UseCase  →  Repository  →  Postgrest (Supabase)
   (UI state)     (StateFlow)   (In→Out)    (interface+impl)
```

Each layer has one job:

| Layer            | Responsibility                                  | Depends on           |
|------------------|-------------------------------------------------|----------------------|
| **DTO**          | Mirror a DB row for (de)serialization           | —                    |
| **Domain model** | Clean type the app/UI uses                      | —                    |
| **Repository**   | Talk to Supabase; return DTOs (or throw)        | `Postgrest`          |
| **Use case**     | One action; map DTO to domain; translate errors | Repository interface |
| **ViewModel**    | Hold UI state; call the use case                | Use-case interface   |
| **Screen**       | Render the state                                | ViewModel            |

Rules of thumb:
- Depend on **interfaces**, never implementations (DI: Hilt binds them).
- ViewModels depend on **use cases**, never repositories directly.
- DTO to domain mapping lives in the **use case**, not the repository.
- Suspend work runs on `Dispatchers.IO` (the repository handles this).

## Package layout

The app is organized **by feature**, not by layer: shared plumbing lives under
`core/`, and each feature is a self-contained package under `feature/<name>/` with
its own `data`/`domain`/`di`/`ui` and its own Hilt module. You add a feature by
creating one new `feature/<name>/` package — you rarely touch other features.

```
com/fullsail/shoppingmadebetter/
├── core/                                   # shared, cross-feature plumbing
│   ├── di/SupabaseModule.kt                # provides SupabaseClient + Postgrest
│   └── domain/UseCase.kt                   # shared UseCase<In, Out> base
├── feature/
│   └── <feature>/
│       ├── data/
│       │   ├── <Feature>Dto.kt             # @Serializable wire model
│       │   ├── <Feature>Repository.kt      # interface
│       │   └── <Feature>RepositoryImpl.kt
│       ├── domain/
│       │   ├── <Feature>.kt                # clean domain model
│       │   ├── Get<Feature>UseCase.kt      # interface + nested Input/Output
│       │   └── Get<Feature>UseCaseImpl.kt
│       ├── di/<Feature>Module.kt           # @Binds for this feature
│       └── ui/
│           ├── <Feature>ViewModel.kt
│           └── <Feature>Screen.kt
└── ...                                     # app scaffolding: MainActivity,
                                            # navigation/, ui/theme/, ui/screens/
```

Interface + impl of the same feature live together in one package (they're small
and owned by one team), so referencing them needs no imports.

---

## Step 1 — DTO (`feature/<name>/data/`)

Mirror the table columns. Postgres uses snake_case, so map any differing names with
`@SerialName`. Only include the columns you need.

`feature/stores/data/StoreDto.kt`:
```kotlin
@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    val address: String,
    @SerialName("postal_code") val postalCode: String,
    val phone: String? = null,
)
```

## Step 2 — Domain model (`feature/<name>/domain/`)

A plain data class the UI consumes — no serialization annotations. Keeps the app
decoupled from the wire format.

`feature/stores/domain/Store.kt`:
```kotlin
data class Store(
    val id: String,
    val name: String,
    val address: String,
    val postalCode: String,
    val phone: String?,
)
```

## Step 3 — Repository (`feature/<name>/data/`)

Interface returns DTOs; the impl injects `Postgrest`, runs on `Dispatchers.IO`, and
**throws** on failure (the use case translates the error).

`feature/stores/data/StoreRepository.kt`:
```kotlin
interface StoreRepository {
    suspend fun getStores(): List<StoreDto>
}
```

`feature/stores/data/StoreRepositoryImpl.kt`:
```kotlin
class StoreRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : StoreRepository {
    override suspend fun getStores(): List<StoreDto> = withContext(Dispatchers.IO) {
        postgrest.from("stores").select().decodeList<StoreDto>()
    }
}
```

Common Postgrest calls:
```kotlin
postgrest.from("table").select().decodeList<Dto>()                 // all rows
postgrest.from("table").select { filter { eq("id", id) } }.decodeSingle<Dto>()
postgrest.from("table").insert(dto)
postgrest.from("table").update(dto) { filter { eq("id", id) } }
postgrest.from("table").delete { filter { eq("id", id) } }
```

## Step 4 — Use case (`feature/<name>/domain/`)

The shared base already exists in `core/` — **do not recreate it**:

`core/domain/UseCase.kt`:
```kotlin
interface UseCase<in Input, out Output> {
    suspend fun execute(input: Input): Output
}
```

Your use case extends it and **nests its own `Input` and a sealed `Output`**. Use
`Unit` as input when there are no arguments.

`feature/stores/domain/GetStoresUseCase.kt`:
```kotlin
interface GetStoresUseCase : UseCase<Unit, GetStoresUseCase.Output> {
    sealed interface Output {
        data class Success(val stores: List<Store>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
```

The impl calls the repository, maps DTO to domain, and turns exceptions into
`Output.Failure`. Log the real cause so failures aren't invisible.

`feature/stores/domain/GetStoresUseCaseImpl.kt`:
```kotlin
class GetStoresUseCaseImpl @Inject constructor(
    private val storeRepository: StoreRepository,
) : GetStoresUseCase {
    override suspend fun execute(input: Unit): GetStoresUseCase.Output =
        try {
            GetStoresUseCase.Output.Success(storeRepository.getStores().map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stores: ${e.message}", e)
            GetStoresUseCase.Output.Failure(e)
        }

    private fun StoreDto.toDomain() = Store(id, name, address, postalCode, phone)

    private companion object { const val TAG = "GetStoresUseCase" }
}
```

## Step 5 — Wire up Hilt (`feature/<name>/di/`)

Each feature owns **one** Hilt module that binds its interfaces to their impls.
Because it's per-feature, teams don't fight over a shared module.

`feature/stores/di/StoresModule.kt`:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class StoresModule {
    @Binds @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository

    @Binds
    abstract fun bindGetStoresUseCase(impl: GetStoresUseCaseImpl): GetStoresUseCase
}
```

The `SupabaseClient`/`Postgrest` singletons are already provided in
`core/di/SupabaseModule.kt` — you don't touch that for a new feature.

## Step 6 — ViewModel (`feature/<name>/ui/`)

Expose a sealed UI state as a `StateFlow`; load in `init` via `viewModelScope`.

`feature/stores/ui/StoresViewModel.kt`:
```kotlin
sealed interface StoresUiState {
    data object Loading : StoresUiState
    data class Success(val stores: List<Store>) : StoresUiState
    data object Error : StoresUiState
}

@HiltViewModel
class StoresViewModel @Inject constructor(
    private val getStoresUseCase: GetStoresUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StoresUiState>(StoresUiState.Loading)
    val uiState: StateFlow<StoresUiState> = _uiState.asStateFlow()

    init { loadStores() }

    fun loadStores() {
        _uiState.value = StoresUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getStoresUseCase.execute(Unit)) {
                is GetStoresUseCase.Output.Success -> StoresUiState.Success(out.stores)
                is GetStoresUseCase.Output.Failure -> StoresUiState.Error
            }
        }
    }
}
```

## Step 7 — Screen (`feature/<name>/ui/`)

`hiltViewModel()` supplies the ViewModel; `collectAsState()` observes the state;
`when` handles every case (loading / error / success).

See `feature/stores/ui/StoresScreen.kt` for the full example (LazyColumn +
loading/error/empty states).

## Step 8 — Navigation (`navigation/Destinations.kt` + `MainActivity.kt`)

Add a route and register the screen:
```kotlin
// Destinations.kt
@Serializable data object Stores : Dest

// MainActivity.kt, inside NavHost { ... }
composable<Dest.Stores> { StoresScreen() }
```
Then give the user a way to reach it (e.g. a button that calls
`navController.navigate(Dest.Stores)`).

## Step 9 — Test the use case

Use a hand-written fake repository — no mocking library needed. Cover the success
mapping and the failure path. See
`app/src/test/.../feature/stores/domain/GetStoresUseCaseTest.kt`.

```kotlin
private class FakeStoreRepository(
    private val result: List<StoreDto> = emptyList(),
    private val error: Throwable? = null,
) : StoreRepository {
    override suspend fun getStores() = error?.let { throw it } ?: result
}

@Test
fun `maps DTOs to domain on success`() = runTest {
    val useCase = GetStoresUseCaseImpl(FakeStoreRepository(result = listOf(/* … */)))
    assertTrue(useCase.execute(Unit) is GetStoresUseCase.Output.Success)
}
```

Run: `./gradlew testDebugUnitTest`.

---

## Gotchas (learned the hard way)

### New table? Add an RLS + grant migration
Migration-created tables have **no RLS and no grants**, so the app's anon key is
**denied `SELECT` by default** — you'll only ever see the error state. For each
table the client reads, add a migration (see
`supabase/migrations/20260704120000_grant_public_read_stores.sql`):
```sql
ALTER TABLE public.<table> ENABLE ROW LEVEL SECURITY;
GRANT SELECT ON public.<table> TO anon, authenticated;
CREATE POLICY "<table> readable by everyone"
  ON public.<table> FOR SELECT TO anon, authenticated USING (true);
```
Apply with `npx supabase migration up` (non-destructive) or `npx supabase db reset`.

### DTO must not choke on extra columns
`select()` returns **all** columns (including `created_at`/`updated_at`). If your
DTO omits some, either select only the columns you need
(`select(Columns.list("id", "name", …))`) or make sure they're optional — a missing
non-optional field will fail deserialization at runtime (caught as `Output.Failure`).

### Logging in code under unit test
`android.util.Log` isn't available in plain JVM unit tests and throws "not mocked".
The project already sets `testOptions { unitTests.isReturnDefaultValues = true }` in
`app/build.gradle.kts`, so `Log.*` calls return defaults instead — keep that in mind
if a use case that logs is under test.

### Config & connecting the emulator
`SUPABASE_URL` / `SUPABASE_ANON_KEY` come from `local.properties` via `BuildConfig`
(never committed). On the emulator, point `SUPABASE_URL` at your host's **LAN IP**
(`http://192.168.1.33:54321`) — `10.0.2.2` is unreliable through Docker Desktop on Windows.
Full setup is in the [README](../README.md).

---

## Checklist

All under `feature/<name>/`:

- [ ] `data/<Feature>Dto.kt` — `@Serializable`, `@SerialName` for snake_case
- [ ] `domain/<Feature>.kt` — clean domain model
- [ ] `data/<Feature>Repository.kt` (interface) + `data/<Feature>RepositoryImpl.kt`
- [ ] `domain/Get<Feature>UseCase.kt` (+ nested `Input`/`Output`) + `Get<Feature>UseCaseImpl.kt`
- [ ] `di/<Feature>Module.kt` — one module with a `@Binds` per interface
- [ ] `ui/<Feature>ViewModel.kt` + `ui/<Feature>Screen.kt`
- [ ] Route added in `navigation/Destinations.kt` + `composable<>` in `MainActivity.kt`
- [ ] Unit test for the use case (`./gradlew testDebugUnitTest` passes)
- [ ] RLS + grant migration if the feature reads a new table
