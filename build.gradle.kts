plugins {
    id("com.android.application") version libs.versions.androidGradlePlugin.get() apply false
    id("com.android.library") version libs.versions.androidGradlePlugin.get() apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.kapt") version libs.versions.kotlin.get() apply false
    id("com.google.devtools.ksp") version libs.versions.ksp.get() apply false
}
