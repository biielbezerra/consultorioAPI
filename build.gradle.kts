plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.consultorioAPI"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.5")
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
