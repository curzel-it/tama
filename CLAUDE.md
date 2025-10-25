# Hello Claude!

## Handling a Task
1. If the task is not too simple, create a plan.md file with a comprehensive list of what you need to change, where, code links and code snippets (especially of data structures)
2. Multiple agents might be working on the codebase at once, so make sure "plan.md" has a name that reflects the tasks at hand to avoid conflicts
3. Ask me any question on things that are uncertain about the plan (you come up with questions in the form of a numbered list)
4. Update the plan with my answers
5. Implement and test frequently
6. Review and cleanup, remove unnecessary comments
7. Enjoy!

## Style and other guideliens
Stick to style and preferences existing in the current codebase.
After implementing any changes run a code review on all edited files.
Run `cargo clippy` for hints on what can be improved.
Before marking the task as completed, make sure you remove all unnecessary comments.

## Testing
All the code you write should be (at least somewhat) unit-testable, in particular game engines.
Make sure to implement all tests that it makes sense to have

Test your implementation by the various executables to spot compilation errors (`cargo build --bin ...`).
Remember to run `cargo test` frequently to make sure you did not introduce breaking changes.

## How to add a new feature
We follow a simple architecture, just one rule: One feature = One File.
When adding a new feature, create a new file for that feature.
Ideally, new features are "wired up" with jus a few lines of code for configuration and setup.

### User Interface
Implement the cleanest and simplest possible version of the ui.

### Architectural preferences on Kotlin Multiplatform
Have dedicated files for `SomethingView`, `SomethingViewModel` and `SomethingUseCase` where applicable.
For example, should we need a special "account" icon with badges, profile image, login/logout states, I would expect something like: ProfileIconView, ProfileIconViewModel and ProfileIconUseCase

## Building the KMP app
You can build and run the Kotlin Multilplatform app like this:
```bash 
cd app && ./gradlew jvmRun -DmainClass=it.curzel.tama.MainKt
```

## Native dependencies in KMP app
Do not ever use actual/expect.
Use a dump dependency-injection approach, for example:

in composeApp/commonMain/SomeUseCase.kt
```
interface SomeDependency {
    fun foo()
}

object SomeUseCase {
    lateinit var someDependency: SomeDependency // Set during app init by native code
}
```

in composeApp/iosMain/SomeDependencyImpl.kt
```
// If needed, this wil lbe implemented in Swift or whatever later
interface SomeDependencyNative {
    fun foo()
}

class SomeDependencyImpl(val thing: SomeDependencyNative) : SomeDependency {
    ...
}
```
in "main" SomeUseCase.someDependency = SomeDependencyImpl(...)