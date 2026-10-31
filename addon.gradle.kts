// Project-specific Gradle customizations belong here.

tasks.withType<Test>().configureEach {
    useJUnit()
}
