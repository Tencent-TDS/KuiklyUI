## ADDED Requirements

### Requirement: Kotlin 2.3.10 SHALL be the default build version

The framework SHALL build with Kotlin `2.3.10` by default: `settings.gradle.kts` SHALL point `buildFileName` at `build.2.3.10.gradle.kts`, so that a plain `./gradlew <task>` resolves to the 2.3.10 scripts without any extra flags, environment variables, or file replacement.

> 注：早期方案要求 2.3.10 与 2.1.21 **并存且互不干扰**（"without modifying the 2.1.21 scripts"、"both version branches SHALL coexist"）。
> 该方案已被取代：目标改为**合入后默认版本即 2.3.10**，不再维护共存切换，配置回归单一版本。本节按最终实现描述。

#### Scenario: Default build resolves to 2.3.10

- **WHEN** a developer runs `./gradlew <task>` with no `-c` flag and no version environment variables
- **THEN** the build SHALL compile with Kotlin `2.3.10` and KSP `2.3.4`.

#### Scenario: No switch mechanism required

- **WHEN** building the default (2.3.10) configuration
- **THEN** the build SHALL NOT require `-c settings.2.3.10.*`, `KUIKLY_AGP_VERSION` / `KUIKLY_KOTLIN_VERSION` environment variables, or `FileReplacer` replacement.

#### Scenario: Historical version scripts are retained but not default

- **WHEN** inspecting the repository
- **THEN** `build.<version>.gradle.kts` / `settings.<version>.gradle.kts` for historical versions (1.3.10 … 2.1.21) MAY remain as historical assets, but SHALL NOT be selected by the default build.
- **AND** because Gradle is pinned to 8.9, historical versions (AGP 7.4.2) SHALL NOT be buildable on this branch — this is an accepted consequence of defaulting to 2.3.10.

---

### Requirement: The build SHALL use the `kotlin_compiler_2_3` compiler-plugin source set

`core-gradle-plugin` SHALL provide a dedicated source set `src/main/kotlin_compiler_2_3/` (`IrTypesExtension`, `KuiklyCommandLineProcessor`, `KuiklyCompilerPluginRegistrar`, `KuiklyGenerationExtension`). When building with 2.3.10, the plugin SHALL compile against this source set.

#### Scenario: Compiler plugin source set selection

- **WHEN** the build version resolves to `2.3.10`
- **THEN** `core-gradle-plugin` SHALL compile `src/main/kotlin_compiler_2_3/`.

---

### Requirement: `2.3.10` SHALL be a selectable build version in the version mapping

`buildSrc/src/main/java/KuiklyKotlinBuildVar.kt` SHALL include a `2.3.10` entry, so that `2.3.10` can be selected as a build version through the same mechanism as existing versions (`1.3.10` … `2.1.21`).

#### Scenario: Version mapping exposes 2.3.10

- **WHEN** the build system resolves the Kotlin version mapping
- **THEN** `2.3.10` SHALL be resolvable and SHALL select the 2.3.10 scripts and the `kotlin_compiler_2_3` plugin source set.

---

### Requirement: The default 2.3.10 build SHALL pin the toolchain to Gradle 8.9 / AGP 8.6.0 / JDK 17

The 2.3.10 configuration SHALL declare AGP `8.6.0` (Kotlin 2.3 requires 8.2.2~8.13.0) and SHALL require Gradle `8.9` (AGP 8.6.0 requires Gradle 8.7+) running on JDK 17.

#### Scenario: Toolchain versions

- **WHEN** building the default configuration
- **THEN** Gradle SHALL be `8.9`, AGP SHALL be `8.6.0`, KSP SHALL be `2.3.4`, and the JVM SHALL be JDK 17.

#### Scenario: CI environment

- **WHEN** the 2.3.10 publish job runs in CI
- **THEN** it SHALL use a JDK 17 environment (generic env steps pinning JDK 11 are incompatible with Gradle 8.9).

---

### Requirement: Build scripts SHALL use the Kotlin 2.3 compiler-options DSL

Build scripts selected by the default build SHALL NOT use Kotlin 2.3-removed APIs. Specifically: `jvmTarget: String`, `moduleName: String?`, `outputFileName`, and `kotlinOptions { freeCompilerArgs += }` SHALL be replaced by `compilerOptions { jvmTarget.set(...) }`, `outputModuleName.set(...)`, `mainOutputFileName.set(...)`, and `compilerOptions.configure { freeCompilerArgs.addAll(...) }` respectively.

#### Scenario: Removed DSL rejected

- **WHEN** a build script selected by the default build uses `jvmTarget = "1.8"`, `moduleName = "..."`, `outputFileName = "..."`, or `kotlinOptions { freeCompilerArgs += ... }`
- **THEN** the build SHALL fail with a script compilation error instructing migration to the `compilerOptions` DSL.

#### Scenario: Migrated DSL accepted

- **WHEN** a build script uses the `compilerOptions` DSL equivalents
- **THEN** the build SHALL succeed under Kotlin 2.3.10.

---

### Requirement: Kotlin/Native link SHALL succeed for 2.3.10 targets

Source constructs that break Kotlin 2.3's K/N IR serialization SHALL be avoided. Specifically, local classes declared inside `@Composable` functions SHALL be hoisted to top level when they trigger a `Not found Idx` failure at link time.

#### Scenario: Local class inside @Composable breaks link on 2.3

- **WHEN** a local class is declared inside a `@Composable` function and the code is compiled for a Kotlin/Native target with Kotlin 2.3
- **THEN** linking MAY fail with `Not found Idx for <function>|<class>(){}[0]`, and the class SHALL be hoisted to top level to resolve it.

---

### Requirement: JS export restrictions SHALL be satisfied

Exported declarations SHALL NOT include constructs Kotlin 2.3 refuses to export. Specifically, a file annotated `@file:JsExport` SHALL NOT declare a `typealias`.

#### Scenario: typealias under @file:JsExport rejected

- **WHEN** a file annotated `@file:JsExport` declares a `typealias`
- **THEN** compilation SHALL fail with `Declaration of such kind (typealias) cannot be exported to JavaScript`.

#### Scenario: Non-exported declarations unaffected

- **WHEN** an interface carries an explicit `@JsExport` annotation in a file whose `@file:JsExport` was removed to satisfy the above
- **THEN** the interface SHALL still be exported under the same JS-visible name.

---

### Requirement: The 2.3.10 default SHALL NOT change framework runtime behaviour or public API

Defaulting to 2.3.10 SHALL affect only toolchain versions and build wiring. No framework runtime behaviour and no business-visible API SHALL differ from the previous default.

#### Scenario: Runtime equivalence

- **WHEN** the framework is built with the 2.3.10 default and run on Android or iOS
- **THEN** pages SHALL render equivalently to the previous default (differences limited to compiler/toolchain behaviour).
