# Kotlin Coroutines SWT

[![Maven Central](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fmaven-central%2Fv%2Fde.brudaswen.kotlinx.coroutines%2Fkotlinx-coroutines-swt%3Fstyle%3Dflat-square)](https://search.maven.org/artifact/de.brudaswen.kotlinx.coroutines/kotlinx-coroutines-swt)
![Snapshot](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fnexus%2Fs%2Fde.brudaswen.kotlinx.coroutines%2Fkotlinx-coroutines-swt%3Flabel%3Dsnapshot%26server%3Dhttps%253A%252F%252Foss.sonatype.org%26style%3Dflat-square)
[![CI Status](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fgithub%2Fworkflow%2Fstatus%2Fbrudaswen%2Fkotlinx-coroutines-swt%2FCI%3Fstyle%3Dflat-square)](https://github.com/brudaswen/kotlinx-coroutines-swt/actions?query=workflow%3ACI)
[![Codecov](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fcodecov%2Fc%2Fgithub%2Fbrudaswen%2Fkotlinx-coroutines-swt%3Fstyle%3Dflat-square)](https://codecov.io/gh/brudaswen/kotlinx-coroutines-swt)
[![License](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fgithub%2Flicense%2Fbrudaswen%2Fkotlinx-coroutines-swt%3Fstyle%3Dflat-square)](https://www.apache.org/licenses/LICENSE-2.0)

Library to easily use kotlinx.coroutines in SWT applications

Provides `Dispatchers.SWT`, `Dispatchers.swt(Display)`, `Dispatchers.swt(Widget)` and `Dispatchers.Main` implementation for SWT UI 
applications.

**NOTE:**
The coroutine dispatcher `Dispatchers.SWT` (or `Dispatchers.Main`) dispatches events to the SWT default display.
Therefore, the SWT default display should be created before calling `Dispatchers.SWT`. Otherwise a new default display
is created (making the thread that invokes `Dispatchers.SWT` its user-interface thread).

## Gradle Dependencies
```kotlin
// Kotlin Coroutines SWT
implementation("de.brudaswen.kotlinx.coroutines:kotlinx-coroutines-swt:1.0.0")

// Platform specific SWT dependency has to be added manually
implementation("org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:3.113.0")
implementation("org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64:3.113.0")
implementation("org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:3.113.0")

// Kotlin Coroutines is added automatically, but can be added to force a specific version
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
```

## Usage
```kotlin
fun main() {
    // Create UI in some thread
    val display = Display.getDefault()
    val shell = Shell(display)
    val label = Label(shell, SWT.NULL)
    // ...

    updateUiInNewThread(display, label)

    while (!display.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
}

fun updateUiInNewThread(display: Display, label: Label) = thread {
    // Dispatch to default display (via [Dispatchers.Main])
    GlobalScope.launch(Dispatchers.Main) {
        label.text = "launch(Dispatchers.Main)"
    }

    // Dispatch to default display (via [Dispatchers.SWT])
    GlobalScope.launch(Dispatchers.SWT) {
        label.text = "launch(Dispatchers.SWT)"
    }

    // Dispatch to given display
    GlobalScope.launch(Dispatchers.swt(display)) {
        label.text = "launch(Dispatchers.swt(display))"
    }

    // Dispatch to display of widget
    GlobalScope.launch(Dispatchers.swt(label)) {
        label.text = "launch(Dispatchers.swt(label))"
    }

    // Dispatch via Display extension
    GlobalScope.launch(display) {
        label.orNull()?.text = "launch(display)"
    }

    // Dispatch via Widget extension
    GlobalScope.launch(label) {
        label.orNull()?.text = "launch(label)"
    }
}
```

## Requirements

| Dependency          | Versions          |
|---                  |---                |
| *Kotlin Coroutines* | 1.1.0 ⁠– 1.3.3     |
| *SWT*               | 3.105.3 ⁠– 3.113.0 |

## License

```
Copyright 2020 Sven Obser

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
