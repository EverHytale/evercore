/*
 * EverCore - Core library for EverHytale plugins
 */

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    // HytaleServer API (provided at runtime)
    // Using local JAR to avoid transitive dependency resolution issues
    compileOnly(files("libs/HytaleServer.jar"))
    
    // JSON Processing
    api("com.google.code.gson:gson:2.10.1")
    
    // HikariCP Connection Pool
    api("com.zaxxer:HikariCP:5.1.0")
    
    // Database drivers
    api("com.h2database:h2:2.2.224")
    runtimeOnly("com.mysql:mysql-connector-j:8.2.0")
    runtimeOnly("org.postgresql:postgresql:42.7.1")
}

group = "fr.everhytale"
version = "1.0.0"
description = "EverCore - Core library for EverHytale plugins"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        
        groupId = "fr.everhytale"
        artifactId = "evercore"
        
        pom {
            name.set("EverCore")
            description.set("Core library for EverHytale plugins")
            url.set("https://github.com/EverHytale/evercore")
            
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            
            developers {
                developer {
                    id.set("everhytale")
                    name.set("EverHytale Team")
                    url.set("https://github.com/EverHytale")
                }
            }
            
            scm {
                connection.set("scm:git:git://github.com/EverHytale/evercore.git")
                developerConnection.set("scm:git:ssh://github.com/EverHytale/evercore.git")
                url.set("https://github.com/EverHytale/evercore")
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/EverHytale/evercore")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
            }
        }
    }
}
