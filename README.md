# Compass

<p align="center">
<b>Kotlin API and tools to make working with Realm easier</b>
</p>

<p align="center"> 
<a href="https://github.com/arunkumar9t2/compass/actions/workflows/ci.yml"><img src="https://img.shields.io/github/workflow/status/arunkumar9t2/compass/CI?logo=GitHub&style=flat-square"/></a>
<a href="https://arunkumar9t2.github.io/compass/"><img src="https://img.shields.io/badge/Website-%20-lightgrey.svg?color=0F8842&colorA=0F8842&style=flat-square&logo=github"/></a>
</p>

## Components

`Compass` is designed to make working with [Realm](https://realm.io) easier through collection of Kotlin types and extensions that handle `Realm`'s lifecycle and threading model effectively. It has two major components

* `compass` - The core Kotlin API with set of extensions for common patterns around `Realm`'s lifecycle and threading.
* `compass-paging` - Provides extensions to integrate with [Jetpack Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview).
* _more coming soon..™_

## Getting Started

`Compass` is available as android library artifacts on `mavenCentral`. In root `build.gradle`:

```groovy
allprojects {
  repositories {
    mavenCentral()
  }
}
```

Or in `dependenciesResolutionManagement` in `settings.gradle`:

```groovy
dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}
```
Then in your modules:

```groovy

dependencies {
    implementation "dev.arunkumar.compass:compass:1.0.0"
    // Paging integration
    implementation "dev.arunkumar.compass:compass-paging:1.0.0"
}
```
## Setup

Compass assumes `Realm.init(this)` and `Realm.setDefaultConfiguration(config)` is called already and acquires a default instance of `Realm` using `Realm.getDefaultInstance()` where needed.

## Features

### Query construction

Use [RealmQuery](https://arunkumar9t2.github.io/compass/compass/dev.arunkumar.compass/-realm-query.html) construction function to build `RealmQuery` instances. Through use of lambdas, `RealmQuery{}` overcomes threading limitations by deferring invocation to usage site rather than call site.

```kotlin
val personQueryBuilder =  RealmQuery { where<Person>().sort(Person.NAME) }
```
Extensions like [getAll()](https://arunkumar9t2.github.io/compass/compass/dev.arunkumar.compass/get-all.html) is provided on `RealmQueryBuilder` that takes advantage of this pattern.

### Threading

`Realm`'s live [updating object](https://docs.mongodb.com/realm/sdk/android/fundamentals/live-queries/#auto-refresh) model mandates few threading rules. Those rules are:

1. `Realm`s can be accessed only from the thread they were originally created
2. `Realm`s can be observed only from threads which have Android's [Looper](https://developer.android.com/reference/android/os/Looper) [prepared](https://developer.android.com/reference/android/os/Looper#prepare()) on them.
3. Managed `RealmResults` can't be passed around the threads.

`Compass` tries to make it easier to work with Realm by providing safe defaults.

#### RealmExecutor/RealmDispatcher

`RealmExecutor` and `RealmDispatcher` are provided which internally prepares Android Looper by using `HandlerThread`s. The following is valid:

```kotlin
withContext(RealmDispatcher()) {
    Realm { realm -> // Acquire default Realm with `Realm {}`
        val persons = realm.where<Person>().findAll()

        val realmChangeListener = RealmChangeListener<RealmResults<Person>> {
            println("Change liseneter called")
        }
        persons.addChangeListener(realmChangeListener) // Safe to add

        // Make a transaction
        realm.transact { // this: Realm
            copyToRealm(Person())
        }
        
        delay(500)  // Wait till change listener is triggered
    } // Acquired Realm automatically closed
}
```
Note that `RealmDispatcher` should be closed when no longer used to release resources. For automatic lifecycle handling via [Flow](https://kotlinlang.org/docs/flow.html), see below.

#### Streams via Flow

`Compass` provides extensions for easy conversions of queries to `Flow` and confirms to basic threading expectations of a `Flow`
* Returned objects can be passed to different threads.
* Handles `Realm` lifecycle until `Flow` collection is stopped.

```kotlin
val personsFlow = RealmQuery { where<Person>() }.asFlow()
```
Internally `asFlow` creates a dedicated `RealmDispatcher` to run the queries and observe changes. The created dispatcher is automatically closed and recreated when collection stops/restarted. By default, all `RealmResults` objects are copied using `Realm.copyFromRealm`.

##### Read subset of data.

Copying large objects from Realm can be expensive in terms of memory, to read only subset of results to memory use `asFlow()` overload that takes a [transform](https://arunkumar9t2.github.io/compass/compass/dev.arunkumar.compass/index.html#-1272439111/Classlikes/1670650899) function.

```kotlin
data class PersonName(val name: String)

val personNames = RealmQuery { where<Person>() }.asFlow { PersonName(it.name) }
```

#### Paging

`Compass` provides extensions on `RealmQueryBuilder` to enable paging support. For example:

```kotlin
val pagedPersons = RealmQuery { where<Person>() }.asPagingItems()
```

`asPagingItems` internally manages a `Realm` instance, run queries using `RealmDispatcher` and cleans up resources when `Flow` collection is stopped.

For reading only subset of objects into memory, use the `asPagingItems()` overload with a `transform` function:

```kotlin
val pagedPersonNames = RealmQuery { where<Person>() }.asPagingItems { it.name }
```

##### ViewModel

For integration with ViewModel


```kotlin
class MyViewModel: ViewModel() {

    val results = RealmQuery { where<Task>() }.asPagingItems().cachedIn(viewModelScope)
}
```

The `Flow` returned by `asPagingItems()` can be safely used for [transformations](https://developer.android.com/topic/libraries/architecture/paging/v3-transform#transform-data-stream), [seperators](https://developer.android.com/topic/libraries/architecture/paging/v3-transform#handle-separators-ui) and [caching](https://developer.android.com/topic/libraries/architecture/paging/v3-transform#avoid-duplicate). Although supported, for [converting to UI model](https://developer.android.com/topic/libraries/architecture/paging/v3-transform#convert-ui-model) prefer using `asPagingItems { /* convert */ }` as it is more efficient.

##### Compose

The `Flow<PagingData<T>>` produced by `asPagingItems()` can be consumed by `Compose` with `collectAsLazyPagingItems()` from `paging-compose`:

```kotlin
val items = tasks.collectAsLazyPagingItems()
  LazyColumn(
    modifier = modifier.padding(contentPadding),
  ) {
    items(
      items = items,
      key = { task -> task.id.toString() }
    ) { task -> taskContent(task) }
  }
```

## FAQ

#### Why not Realm Freeze?

[Frozen](https://docs.mongodb.com/realm/sdk/android/advanced-guides/threading/#frozen-objects) Realm objects is the official way to safely move objects around threads. However it still says connected to udnerlying `Realm` and poses risk around threading.

> Frozen objects remain valid for as long as the realm that spawned them stays open. Avoid closing realms that contain frozen objects until all threads are done working with those frozen objects.

`Compass`'s [transform](https://arunkumar9t2.github.io/compass/compass/dev.arunkumar.compass/index.html#-1272439111/Classlikes/1670650899) API supports both creating detached objects from `Realm` and reading subset of `Realm` object into memory.

## Resources

TBA