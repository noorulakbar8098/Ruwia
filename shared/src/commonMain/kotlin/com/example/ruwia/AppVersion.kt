package com.example.ruwia

/**
 * Shown when the platform cannot report its own version (Compose previews,
 * non-Android targets). Keep in sync with the androidApp `versionName`
 * (`versionCode`) on every release.
 */
const val appVersionFallbackLabel = "v2.2 (4)"

/**
 * Human-readable app version, e.g. "v2.2 (4)". Android reads the real
 * `PackageManager` values so the label can never drift from Gradle; other
 * targets use [appVersionFallbackLabel].
 */
expect fun appVersionLabel(): String
