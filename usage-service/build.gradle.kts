plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web") // For analytics endpoints
    implementation("org.springframework.boot:spring-boot-starter-webflux") // For WebClient
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(project(":common"))
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // JUnit QuickCheck for property-based testing
    testImplementation("com.pholser:junit-quickcheck-core:1.0")
    testImplementation("com.pholser:junit-quickcheck-generators:1.0")
}
