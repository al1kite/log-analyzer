plugins {
    java
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.electricip"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Cache - Caffeine (비용 절감: API 호출 최소화)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // CSV Parsing - Apache Commons CSV (안정성과 성능 검증됨)
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("commons-io:commons-io:2.16.1")

    // OpenAPI/Swagger (API 문서화)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Lombok (보일러플레이트 코드 제거)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Configuration Properties 메타데이터 자동 생성
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.bytebuddy:byte-buddy:1.15.10")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.15.10")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
