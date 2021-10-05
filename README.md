# Compass

<p align="center">
<b>Kotlin API and tools to make working with Realm easier</b>

<a href="https://github.com/arunkumar9t2/compass/actions/workflows/ci.yml"><img src="https://img.shields.io/github/workflow/status/arunkumar9t2/compass/CI?logo=GitHub&style=flat-square"/></a>
</p>

## Components

`Compass` is designed to make working with [Realm](https://realm.io) easier through collection of Kotlin types and extensions that handle `Realm`'s lifecycle and threading model effectively. It has two major components

* `compass` - The core Kotlin API with set of extensions for common patterns around `Realm`'s lifecycle and threading.
* `compas-paging` - Provides extensions to integrate with [Jetpack Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview).
* _more coming soon.._

## Getting Started

`Compass` is available as android library artifacts on `mavenCentral`. Ensure `mavenCentral` is added to root `build.gradle` file as shown:

```groovy
allprojects {
  repositories {
    mavenCentral()
  }
}
```

Or in `dependenciesResolutionManagement` when using `Settings` API in `settings.gradle`:

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
## Features

### Query construction

`Compass` provides [RealmQuery](https://arunkumar9t2.github.io/compass/compass/dev.arunkumar.compass/-realm-query.html) construction function to build `RealmQuery` instances. The construction function returns `RealmQueryBuilder<T>` which is a typealias for `Realm.() -> RealmQuery<T>`. Through use of lambdas, compass overcomes threading limitations by deferring invocation to usage site rather than call site.

```kotlin
val personQueryBuilder =  RealmQuery { where<Person>().sort(Person.NAME) }
```
Extensions like [getAll()](https://arunkumar9t2.github.io/compass/compass/dev.arunkumar.compass/get-all.html) is provided on `RealmQueryBuilder` that takes advantage of this pattern.

### Threading

`Realm`'s live [updating object](https://docs.mongodb.com/realm/sdk/android/fundamentals/live-queries/#auto-refresh) model mandates few threading rules. Those rules are:

1. `Realm`s can be accessed only from the thread they were originally created
2. `Realm`s can be observed only from threads which have [Android](https://developer.android.com/reference/android/os/Looper)'s Looper [prepared](https://developer.android.com/reference/android/os/Looper#prepare()) on them.
3. `RealmResults` also can't be passed around the threads.

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

Compass provides extensions to convert a `RealmQueryBuilder` to `Flow<T>` and confirms to basic threading expectations of a `Flow`
* Returned objects can be passed to different threads
* Handles `Realm` lifecycle until `Flow` collection is stopped.

```kotlin
val personsFlow = RealmQuery { where<Person>() }.asFlow()
```
Internally `asFlow` creates a dedicated `RealmDispatcher` to run the queries and observe changes. The created dispatcher is automatically closed and recreated when collection stops/restarted.