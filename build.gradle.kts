plugins {
    java
    jacoco
    pmd
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.owasp.dependencycheck") version "12.2.2"
    id("com.google.protobuf") version "0.9.6"
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "yomu-backend-java"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

pmd {
    toolVersion = "7.0.0"
    isConsoleOutput = true
    rulesMinimumPriority = 5
}

tasks.withType<Pmd>().configureEach {
    exclude("**/proto/**")
    ignoreFailures = false
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencyCheck {
    failBuildOnCVSS = 9.0f
    formats = listOf("HTML", "SARIF")
    scanConfigurations = listOf("runtimeClasspath")
    skipConfigurations = listOf("testRuntimeClasspath", "testCompileClasspath")
    skipTestGroups = true
    data {
        directory = providers
            .systemProperty("org.owasp.dependencycheck.data.directory")
            .orElse(providers.environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY"))
            .orElse(file(".gradle/dependency-check-data").absolutePath)
            .get()
    }
    nvd {
        apiKey = System.getenv("NVD_API_KEY").orEmpty()
    }
    analyzers {
        assemblyEnabled = false
        nugetconfEnabled = false
        msbuildEnabled = false
        nodeAudit {
            enabled.set(false)
        }
        ossIndex {
            enabled.set(false)
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("com.h2database:h2")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.7")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.7")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.7")
    testImplementation("org.springframework.security:spring-security-test")

    implementation(platform("org.springframework.grpc:spring-grpc-dependencies:1.0.3"))
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("com.google.protobuf:protobuf-java")
    compileOnly("jakarta.annotation:jakarta.annotation-api:1.3.5")
    implementation("io.grpc:grpc-services")
    testImplementation("org.springframework.grpc:spring-grpc-test")
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "${protobuf.generatedFilesBaseDir}/main/java", "${protobuf.generatedFilesBaseDir}/main/grpc")
        }
        proto {
            srcDir("${projectDir}/src/main/proto/yomu")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.33.4"
    }
    plugins {
        register("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.77.1"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

tasks.compileJava {
    dependsOn("generateProto")
}

tasks.test{
    filter{
        excludeTestsMatching("*FunctionalTest")
    }

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}
