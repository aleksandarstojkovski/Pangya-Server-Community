plugins {
    application
}

application {
    mainClass.set("org.pangya.messenger.MessengerServerMain")
    applicationName = "server-messenger"
}

dependencies {
    implementation(project(":core-network"))
    implementation(project(":core-db"))
    implementation(libs.bundles.logging)
    implementation(libs.jedis)
    implementation(libs.micrometer.prometheus)
}
