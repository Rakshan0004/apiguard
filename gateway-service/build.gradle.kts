plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway:4.1.5")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation(project(":common"))
}
