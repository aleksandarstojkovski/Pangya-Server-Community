plugins {
    `java-library`
}

dependencies {
    api(project(":core-protocol"))
    api(libs.slf4j.api)
    api(libs.netty.all)
    api(libs.snakeyaml)
    api(libs.micrometer.core)
    api(libs.jedis)
    implementation(libs.bundles.logging)
    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
