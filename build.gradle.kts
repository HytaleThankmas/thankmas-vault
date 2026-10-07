plugins {
    id("gg.ginco.hygradle")
    id("com.google.devtools.ksp") version "2.3.6"
    kotlin("jvm") version "2.3.21"
}

version = "1.0.0"

hytale {
    group = "dev.rm20"
    name = "ThankmasVault"
    description = "An Economy plugin"
    mainClass = "dev.rm20.thankmasvault.ThankmasVault"
    author("Hytale Thankmas Team")

    bundleDependencies = true
    includesAssetPack = true
    // serverVersion inherited from hytaleWorkspace
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("gg.ginco:hytale-codec-annotations:1.2.1")
    implementation("gg.ginco:hytale-codec-runtime:1.2.1")
    ksp("gg.ginco:hytale-codec-processor:1.2.1")
    // implementation("com.github.rm20killer:CodecAnnotation:ac9cf6574a")
    compileOnly(files("../../libs/AnglersAlmanac-1.2.2-Thankmas.jar"))
}