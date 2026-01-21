import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

// --- 1. CONFIGURATION DU SCRIPT DE BUILD ---
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Pour envoyer des mails depuis Gradle
        classpath("com.sun.mail:javax.mail:1.6.2")
    }
}

// --- 2. PLUGINS ---
plugins {
    java
    id("com.github.spacialcircumstances.gradle-cucumber-reporting") version "0.1.25"
    jacoco
    id("org.sonarqube") version "5.0.0.4638"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

// --- 3. DÉPÔTS (Où télécharger les librairies) ---
repositories {
    mavenCentral() // INDISPENSABLE pour télécharger JUnit

    // Votre dépôt privé (pour télécharger des dépendances privées si besoin)
    maven {
        url = uri("https://mymavenrepo.com/repo/cEmjfkxugPlzLxXg1A2B/")
        credentials {
            username = "myMavenRepo"
            password = "test0005"
        }
    }
}

// --- 4. DÉPENDANCES ---
dependencies {
    // JUnit 5 (Jupiter)
    testImplementation(platform("org.junit.jupiter:junit-jupiter:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Ajoutez ici vos autres dépendances (Cucumber, etc.) si nécessaire
}

// --- 5. CONFIGURATION SONARQUBE ---
sonarqube {
    properties {
        property("sonar.projectKey", "ogl-cucumber-project")
        property("sonar.projectName", "Projet OGL Cucumber")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

// --- 6. CONFIGURATION JACOCO ---
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoHtml"))
    }
}

// --- 7. CONFIGURATION CUCUMBER ---
cucumberReports {
    outputDir = file("build/reports/cucumber-html")
    buildId = "0"
    reports = files("build/reports/cucumber/report.json")
}

// --- 8. TÂCHE DE TEST ---
tasks.test {
    // IMPORTANT : Puisque vous importez junit-jupiter, utilisez useJUnitPlatform()
    useJUnitPlatform()

    ignoreFailures = true // Pour ne pas bloquer la génération des rapports en cas d'erreur

    finalizedBy(tasks.jacocoTestReport)
    finalizedBy("generateCucumberReports")
}

// Lier Sonar à Test
tasks.named("sonar") {
    dependsOn(tasks.test)
}

// --- 9. PUBLISHING (Déploiement) ---
publishing {
    repositories {
        maven {
            // URL définie via variable d'environnement (par Jenkins) ou vide par défaut
            url = uri(System.getenv("MYMAVENREPO_URL") ?: "https://mymavenrepo.com/repo/cEmjfkxugPlzLxXg1A2B/")
            credentials {
                username = System.getenv("MYMAVENREPO_USER") ?: "myMavenRepo"
                password = System.getenv("MYMAVENREPO_PASS") ?: "test0005"
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

// --- 10. TÂCHES DE NOTIFICATION (Slack & Mail) ---

tasks.register("notifySlack") {
    doLast {
        val webhookUrl = "https://hooks.slack.com/services/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"
        val message = """ { "text": "🚀 *Succès !* Le déploiement du projet *${project.name}* (v${project.version}) est terminé." } """.trimIndent()

        try {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            println("Slack response: ${response.statusCode()}")
        } catch (e: Exception) {
            println("Erreur Slack: ${e.message}")
        }
    }
}

tasks.register("sendMail") {
    doLast {
        val host = "smtp.gmail.com"
        val port = "587"
        val myEmail = "mo_lebna@esi.dz"
        val myPassword = "jexk fkfw ozwx obem"
        val toEmail = "h_mokeddem@esi.dz"

        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = host
        props["mail.smtp.port"] = port
        props["mail.smtp.ssl.trust"] = "smtp.gmail.com"

        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                return javax.mail.PasswordAuthentication(myEmail, myPassword)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(myEmail))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            message.subject = "🚀 Déploiement réussi : ${project.name} v${project.version}"
            message.setText("Succès du déploiement sur MyMavenRepo.")
            Transport.send(message)
            println("✅ Email envoyé !")
        } catch (e: Exception) {
            println("❌ Erreur Mail : ${e.message}")
        }
    }
}

// Lier les notifications à la fin du publish
tasks.publish {
    finalizedBy("notifySlack")
    finalizedBy("sendMail")
}