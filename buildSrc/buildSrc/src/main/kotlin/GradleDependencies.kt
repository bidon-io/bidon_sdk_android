object GradleDependencies {

    object Kotlin {
        const val version = "1.8.0"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"

        object Serialization {
            const val gradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:$version"
        }
    }

    object Android {
        const val gradlePlugin = "com.android.tools.build:gradle:7.4.0"
    }
}