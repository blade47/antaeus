plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    api(project(":pleo-antaeus-models"))
}