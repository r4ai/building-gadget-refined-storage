import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    idea
    `maven-publish`
    id("net.neoforged.moddev") version "1.0.11"
    kotlin("jvm") version "2.3.0"
}

val modId = property("mod_id") as String
val modName = property("mod_name") as String
val modVersion = property("mod_version") as String
val modGroup = property("mod_group_id") as String
val minecraftVersion = property("minecraft_version") as String
val neoVersion = property("neo_version") as String
val kotlinForForgeVersion = property("kotlin_for_forge_version") as String
val rsVersion = property("rs_version") as String
val bg2ProjectId = property("bg2_project_id") as String
val bg2FileId = property("bg2_file_id") as String
val mekanismProjectId = property("mekanism_project_id") as String
val mekanismFileId = property("mekanism_file_id") as String

version = modVersion
group = modGroup

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

neoForge {
    version = neoVersion

    runs {
        configureEach {
            logLevel = org.slf4j.event.Level.INFO
            systemProperty("forge.logging.console.level", "info")
        }

        create("client") {
            client()
            jvmArgument("-Xms1G")
            jvmArgument("-Xmx4G")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            data()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath
            )
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }
    test {
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().runtimeClasspath + output
    }
}

configurations {
    create("localRuntime")
    runtimeClasspath {
        extendsFrom(configurations["localRuntime"])
    }
}

repositories {
    mavenLocal()
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "Kotlin for Forge"
        content {
            includeGroup("thedarkcolour")
        }
    }
    maven("https://maven.creeperhost.net") {
        name = "Refined Architect"
        content {
            includeGroupAndSubgroups("com.refinedmods")
        }
    }
    ivy("https://github.com/refinedmods/refinedstorage2/releases/download") {
        name = "Refined Storage Releases"
        patternLayout {
            artifact("v[revision]/[artifact]-[revision].[ext]")
        }
        metadataSources {
            artifact()
        }
        content {
            includeGroup("com.refinedmods.refinedstorage")
        }
    }
    maven("https://www.cursemaven.com") {
        name = "CurseMaven"
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForForgeVersion")

    compileOnly("com.refinedmods.refinedstorage:refinedstorage-core-api:$rsVersion")
    compileOnly("com.refinedmods.refinedstorage:refinedstorage-resource-api:$rsVersion")
    compileOnly("com.refinedmods.refinedstorage:refinedstorage-storage-api:$rsVersion")
    compileOnly("com.refinedmods.refinedstorage:refinedstorage-autocrafting-api:$rsVersion")
    compileOnly("com.refinedmods.refinedstorage:refinedstorage-network-api:$rsVersion")
    compileOnly("com.refinedmods.refinedstorage:refinedstorage-common-api:$rsVersion")
    compileOnly("com.refinedmods.refinedstorage:refinedstorage-neoforge-api:$rsVersion")

    add("localRuntime", "com.refinedmods.refinedstorage:refinedstorage-neoforge:$rsVersion")
    add("localRuntime", "curse.maven:building-gadgets-2-$bg2ProjectId:$bg2FileId")
    add("localRuntime", "curse.maven:mekanism-$mekanismProjectId:$mekanismFileId")

    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "minecraft_version_range" to project.property("minecraft_version_range"),
        "neo_version" to project.property("neo_version"),
        "neo_version_range" to project.property("neo_version_range"),
        "loader_version_range" to project.property("loader_version_range"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_license" to project.property("mod_license"),
        "mod_version" to project.property("mod_version"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description"),
    )
    inputs.properties(replaceProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = modId
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
