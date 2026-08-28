plugins {
    application
}

application {
    mainClass.set("org.pangya.login.LoginServerMain")
    applicationName = "server-login"
}

dependencies {
    implementation(project(":core-network"))
    implementation(project(":core-db"))
    implementation(libs.bundles.logging)
    implementation(libs.jedis)
    implementation(libs.micrometer.prometheus)
}
