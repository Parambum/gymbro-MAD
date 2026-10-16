// Plugins are declared here and applied in :app, so every module in the repo
// agrees on one version. Versions live in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
