plugins {
    application
}

application {
    mainClass.set("org.pangya.game.GameServerMain")
    applicationName = "server-game"
}

dependencies {
    implementation(project(":core-network"))
    implementation(project(":core-db"))
    implementation(libs.bundles.logging)
    implementation(libs.jedis)
    implementation(libs.micrometer.prometheus)
    testImplementation(project(":server-ranking"))
    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
