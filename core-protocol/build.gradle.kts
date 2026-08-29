plugins {
    `java-library`
}

dependencies {
    api(libs.slf4j.api)
    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
