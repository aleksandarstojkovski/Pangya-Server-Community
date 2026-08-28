plugins {
    application
}

application {
    mainClass.set("org.pangya.ranking.RankingServerMain")
    applicationName = "server-ranking"
}

dependencies {
    implementation(project(":core-network"))
    implementation(project(":core-db"))
    implementation(libs.bundles.logging)
    implementation(libs.jedis)
    implementation(libs.micrometer.prometheus)
}
