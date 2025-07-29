@file:Suppress("UNUSED_VARIABLE")

import com.bedalton.gradle.multiplatform.RenameNativeExecutableTask
import com.bedalton.gradle.multiplatform.capitalize
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.bedalton.multiplatform") version "1.0.0"
    kotlin("multiplatform")
    id("org.graalvm.buildtools.native") version "0.9.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

// Kotlin / KotlinX
val kotlinVersion: String by project
val kotlinxCliVersion: String by project
val coroutinesVersion: String by project

// Creatures
val creaturesCommonVersion: String by project
val creaturesCommonCLIVersion: String by project
//val creaturesCommonSpriteVersion: String by project
val creaturesRendererVersion: String by project
val creaturesC2EggParserVersion: String by project
val creaturesPrayDataVersion: String by project
val creaturesMinimalExportParser: String by project

// Libs
val bedaltonCommonCoreVersion: String by project
val bedaltonLocalFilesVersion: String by project
val bedaltonAppSupportVersion: String by project
val bedaltonCommonLogVersion: String by project
val bedaltonCommonByteVersion: String by project
val bedaltonCommonCoroutinesVersion: String by project
val korImagesVersion: String by project

val projectVersion: String by project
val projectGroup: String by project

group = projectGroup
version = projectVersion

bedaltonConfig {
    jsFallback = "node"
    jsTestFallback = "node"
    additionalLibraries = listOf("local-files")
}

application {
    mainClass.set("com.bedalton.creatures.breed.render.cli.MainKt")
}

repositories {
    mavenLocal()
    mavenCentral()
    @Suppress("DEPRECATION")
    jcenter()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        compilations["main"].apply {
            outputModuleName = "breed-renderer-cli"
            packageJson {
                name = "@bedalton/breed-renderer-cli"
                description = "Command line utility for rendering creature breeds"
                customField("license", "MIT")
                customField(
                    "repository", mapOf(
                        "type" to "git",
                        "url" to "git+https://github.com/bedalton/Creatures-BreedRenderer.git"
                    )
                )
                customField("bin", mapOf("render-breed" to "./render-breed-main.js"))
                customField("author", "bedalton")
                customField("main", "breed-renderer-cli.js")
                customField("types", "breed-renderer-cli.d.ts")
                customField("version", projectVersion)
                customField("bugs", mapOf("url" to "https://github.com/bedalton/Creatures-BreedRenderer/issues"))
                customField("homepage", "https://github.com/bedalton/Creatures-BreedRenderer")
                customField(
                    "scripts", mapOf(
                        "package" to "npm install && pkg package.json"
                    )
                )
                customField(
                    "file", mapOf(
                        "breed-renderer" to "breed-renderer.js"
                    )
                )
                customField(
                    "types", "breed-renderer.d.ts"
                )
                customField(
                    "pkg", mapOf(
                        "assets" to listOf<String>(
                            // None right now
                        ),
                        "scripts" to listOf(
                            "./render-breed-main.js"
                        ),
                        "output" to "render-breed.exe",
                        "targets" to listOf("node12-win-x64")
                    )
                )
                customField(
                    "keywords", listOf(
                        "Creatures",
                        "C16",
                        "S16",
                        "SPR",
                        "Creatures1",
                        "Creatures2",
                        "Creatures3",
                        "Docking Station",
                        "DockingStation",
                        "Sprite",
                        "ATT",
                        "Breed Renderer",
                        "Breeds"
                    )
                )
            }
        }
        nodejs {
            useCommonJs()
            binaries.library()
        }
    }
    linuxX64 {
        binaries.executable {
            this.entryPoint = "com.bedalton.creatures.breed.render.cli.main"
        }
    }
    linuxArm64 {
        binaries.executable {
            this.entryPoint = "com.bedalton.creatures.breed.render.cli.main"
        }
    }
    mingwX64 {
        binaries.executable {
            this.entryPoint = "com.bedalton.creatures.breed.render.cli.main"
        }
    }
    macosX64 {
        binaries.executable {
            this.entryPoint = "com.bedalton.creatures.breed.render.cli.main"
        }
    }
    macosArm64 {
        binaries.executable {
            this.entryPoint = "com.bedalton.creatures.breed.render.cli.main"
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin / KotlinX
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.cli)

                implementation(libs.bedalton.app.support)
                implementation(libs.bedalton.common.byte)
                implementation(libs.bedalton.common.core)
                implementation(libs.bedalton.common.coroutines)
                implementation(libs.bedalton.common.files)
                implementation(libs.bedalton.common.log)

                implementation(libs.bedalton.creatures.breed.renderer)
                implementation(libs.bedalton.creatures.common.cli)
                implementation(libs.bedalton.creatures.c2eggparser)
                implementation(libs.bedalton.creatures.pray.data)
                implementation(libs.bedalton.creatures.export.parser)


            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.nodejs)
                implementation(npm("glob", libs.versions.npm.glob.get()))
                implementation(npm("pako", libs.versions.npm.pako.get()))
            }
        }
        val jsTest by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
        val mingwX64Main by getting
        val mingwX64Test by getting
        val macosX64Main by getting
        val macosX64Test by getting


        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
            languageSettings.optIn("kotlin.ExperimentalMultiplatform")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.coroutines.DelicateCoroutinesApi")
            languageSettings.optIn("kotlinx.cli.ExperimentalCli")
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
        }
    }
}


listOf(
    "mingwX64",
    "macosX64",
    "macosArm64",
    "linuxX64",
    "linuxArm64"
).forEach { target ->
    tasks.create("rename${target.capitalize()}Executable", RenameNativeExecutableTask::class.java) {
        group = "native"
        this.target = target
        originalExecutableName = "breed-renderer-cli"
        targetExecutableName = "render-creature"
        outputs.upToDateWhen { false }
    }
}



tasks {

    test {
        exclude("module-info.java")
    }

    compileJava {

        val javaModuleName: String by project
        modularity.inferModulePath.set(true)
        inputs.property("moduleName", javaModuleName)
        targetCompatibility = "17"
        sourceCompatibility = "17"
        doFirst {
            options.compilerArgs = listOf(
                "--module-path", classpath.asPath,
                "--patch-module", "$javaModuleName=${sourceSets["main"].output.asPath}"
            )
            classpath = files()
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
val compileKotlinJvm: KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileKotlinJvm.destinationDirectory.set(compileJava.destinationDirectory.get())

graalvmNative {
    binaries {
        named("main") {
            this.imageName.set("render-creature")
            buildArgs.addAll(
                listOf(
                    "-H:DashboardDump=render-breed-cli",
                    "-H:+DashboardAll",
                    "-H:+PrintClassInitialization",
                )
            )
        }
    }
}

val graalNativeTaskRegex = "native(Compile|Run|TestCompile)".toRegex(RegexOption.IGNORE_CASE)
tasks.filter { graalNativeTaskRegex.matches(it.name) }.forEach {
    it.group = "native"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes("Main-Class" to "com.bedalton.creatures.breed.render.cli.MainKt")
    }
    archiveClassifier.set("all")
    val main by kotlin.jvm().compilations
    from(main.output)
    configurations += main.compileDependencyFiles as Configuration
    configurations += main.runtimeDependencyFiles as Configuration
}

val allNativeTargets = listOf(
    "LinuxArm64",
    "LinuxX64",
    "MacosArm64",
    "MacosX64",
    "MingwX64"
)

fun groupNativeCompileTasks(kind: String) {
    for (target in allNativeTargets) {
        val task = tasks.getByName("link${kind.capitalize()}Executable${target.capitalize()}")
        task.group = "kotlin native ${kind.toLowerCase()}"
    }
}


fun registerCompileAllNativeTargetsTask(kind: String, targets: List<String> = allNativeTargets): TaskProvider<Task> {
    return tasks.register("compileAll${kind.capitalize()}NativeTargets") {
        group = "kotlin native ${kind.toLowerCase()}"
        for (target in targets) {
            val task = tasks.getByName("link${kind.capitalize()}Executable${target.capitalize()}")
            dependsOn(task)
        }
    }
}

groupNativeCompileTasks("release")
groupNativeCompileTasks("debug")
registerCompileAllNativeTargetsTask("release")
registerCompileAllNativeTargetsTask("debug")