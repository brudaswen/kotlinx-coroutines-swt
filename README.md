# Kotlin Coroutines SWT

[![Maven Central](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fmaven-central%2Fv%2Fde.brudaswen.kotlinx.coroutines%2Fkotlinx-coroutines-swt%3Fstyle%3Dflat-square)](https://mvnrepository.com/artifact/de.brudaswen.kotlinx.coroutines/kotlinx-coroutines-swt)
![Snapshot](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fnexus%2Fs%2Fde.brudaswen.kotlinx.coroutines%2Fkotlinx-coroutines-swt%3Flabel%3Dsnapshot%26server%3Dhttps%253A%252F%252Foss.sonatype.org%26style%3Dflat-square)
[![CI Status](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fgithub%2Fworkflow%2Fstatus%2Fbrudaswen%2Fkotlinx-coroutines-swt%2FCI%3Fstyle%3Dflat-square)](https://github.com/brudaswen/kotlinx-coroutines-swt/actions?query=workflow%3ACI)
[![License](https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=https%3A%2F%2Fimg.shields.io%2Fgithub%2Flicense%2Fbrudaswen%2Fkotlinx-coroutines-swt%3Fstyle%3Dflat-square)](https://www.apache.org/licenses/LICENSE-2.0)

Library to easily use kotlinx.coroutines in SWT applications

Provides `Dispatchers.SWT` context, `Dispatchers.swt(Display)` context and `Dispatchers.Main` implementation for SWT UI 
applications.

The coroutine dispatcher `Dispatchers.SWT` (or `Dispatchers.Main`) dispatches events to the SWT default display.
Therefore, the SWT default display should be created before calling `Dispatchers.SWT`. Otherwise a new default display
is created (making the thread that invokes `Dispatchers.SWT` its user-interface thread).

## Dependency

## Usage

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
