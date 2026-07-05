# Jakefile — IntelliJ plugin build/test/verify/publish (jakefile.dev).
#
# `@rooted` resolves relative paths against THIS repo so a workspace meta-repo can
# `@import "intellij-sema/Jakefile" as intellij` and run `intellij.build` from root.
# Gradle (org.jetbrains.intellij.platform) emits build/distributions/Sema-<ver>.zip.
@rooted

@group ext
@desc "Build the IntelliJ plugin (.zip in build/distributions)"
@needs java "install a JDK 17+"
task build:
    ./gradlew buildPlugin

@group ext
@desc "Run the IntelliJ plugin unit tests"
@needs java
task test:
    ./gradlew test

@group ext
@desc "Run the IntelliJ plugin verifier (marketplace compatibility)"
@needs java
task verify:
    ./gradlew verifyPlugin

# Signing + upload token come from the env (see RELEASING.md): PUBLISH_TOKEN,
# plus CERTIFICATE_CHAIN / PRIVATE_KEY / PRIVATE_KEY_PASSWORD.
@group ext
@desc "Publish the IntelliJ plugin to the JetBrains Marketplace"
@needs java
@require PUBLISH_TOKEN
task publish: [build]
    @confirm "Publish the IntelliJ plugin to the JetBrains Marketplace?"
    ./gradlew publishPlugin
