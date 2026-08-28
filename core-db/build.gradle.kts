plugins {
    `java-library`
}

dependencies {
    api(libs.slf4j.api)
    api(libs.bundles.jdbi)
    api(libs.bundles.flyway)
    api(libs.postgresql)
    api(libs.hikari)
    implementation(libs.bundles.logging)
    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
