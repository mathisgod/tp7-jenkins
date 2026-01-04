
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

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // On charge la librairie JavaMail pour que le script puisse l'utiliser
        classpath("com.sun.mail:javax.mail:1.6.2")
    }
}

plugins {
    java
    id("com.github.spacialcircumstances.gradle-cucumber-reporting") version "0.1.25"
    jacoco
    id("org.sonarqube") version "5.0.0.4638"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral() // Pour les libs standards (JUnit, etc.)

    // Votre dépôt privé MyMavenRepo
    maven {
        // L'URL de votre repo (la même que pour le publish)
        url = uri("https://mymavenrepo.com/repo/cEmjfkxugPlzLxXg1A2B/")

        credentials {
            username = "myMavenRepo"
            password = "test0005"
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "ogl-cucumber-project") // ID unique du projet
        property("sonar.projectName", "Projet OGL Cucumber")
        property("sonar.host.url", "http://localhost:9000")  // URL par défaut en local

        // IMPORTANT : Indiquer à Sonar où trouver le rapport XML de JaCoCo pour la couverture
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}
publishing {
    repositories {
        publishing {
            repositories {
                maven {
                    // En Kotlin, on utilise des guillemets doubles " " et on convertit en uri()
                    url = uri(System.getenv("MYMAVENREPO_URL") ?: "")
                    credentials {
                        username = System.getenv("MYMAVENREPO_USER")
                        password = System.getenv("MYMAVENREPO_PASS")
                    }
                }
            }
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }

        dependencies {
            testImplementation(platform("org.junit.jupiter:junit-jupiter:5.9.1"))
            testImplementation("org.junit.jupiter:junit-jupiter")
        }

// Configuration de JaCoCo (Version de l'outil)
        jacoco {
            toolVersion = "0.8.12"
        }


// Configuration du plugin de Reporting Cucumber
// Note: En Kotlin DSL, on utilise configure<Type> pour certains plugins tiers
        cucumberReports {
            outputDir = file("build/reports/cucumber-html")
            buildId = "0"
            reports = files("build/reports/cucumber/report.json")
        }

// Configuration de la tâche de Test
        tasks.test {
            // IMPORTANT : On utilise useJUnit() car vous avez des dépendances JUnit 4
            useJUnit()

            // On ignore les échecs pour que les rapports se génèrent même si un test plante
            ignoreFailures = false

            // Automatisation : Lancer les rapports après les tests
            finalizedBy(tasks.jacocoTestReport)
            finalizedBy("generateCucumberReports")
        }

// Configuration du rapport JaCoCo
        tasks.jacocoTestReport {
            dependsOn(tasks.test) // S'assure que les tests ont tourné

            reports {
                xml.required.set(true)
                csv.required.set(false)
                // En Kotlin DSL, on utilise .set() pour les propriétés
                html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoHtml"))
            }
        }
        tasks.named("sonar") {
            dependsOn(tasks.test)
        }
        tasks.register("notifySlack") {
            doLast {
                // COLLEZ VOTRE URL WEBHOOK SLACK ICI
                val webhookUrl = "https://hooks.slack.com/services/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"

                // Le message à envoyer (JSON)
                val message = """
            {
                "text": "🚀 *Succès !* Le déploiement du projet *${project.name}* (v${project.version}) est terminé."
            }
        """.trimIndent()

                // Envoi de la requête
                val client = HttpClient.newBuilder().build()
                val request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(message))
                        .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    println("Notification Slack envoyée avec succès !")
                } else {
                    println("Erreur Slack : ${response.statusCode()} - ${response.body()}")
                }
            }
        }
        tasks.register("sendMail") {
            doLast {
                // --- CONFIGURATION ---
                val host = "smtp.gmail.com"
                val port = "587"
                val myEmail = "mo_lebna@esi.dz" // VOTRE GMAIL
                val myPassword = "jexk fkfw ozwx obem" // VOTRE MOT DE PASSE D'APPLICATION (pas le mdp normal)
                val toEmail = "h_mokeddem@esi.dz" // L'email du destinataire (équipe dev)

                // --- PRÉPARATION DU MESSAGE ---
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = host
                props["mail.smtp.port"] = port
                // Pour éviter les erreurs de certificats SSL parfois rencontrées en entreprise/école
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
                    message.setText("Bonjour l'équipe,\n\n" +
                            "La version ${project.version} de la librairie ${project.name} a été déployée avec succès sur MyMavenRepo.\n\n" +
                            "Cordialement,\nLe Bot Gradle.")

                    // --- ENVOI ---
                    Transport.send(message)
                    println("✅ Email de notification envoyé avec succès à $toEmail !")

                } catch (e: Exception) {
                    println("❌ Erreur lors de l'envoi du mail : ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        tasks.publish {
            finalizedBy("notifySlack")
        }
        tasks.publish {
            finalizedBy("sendMail")
        }
    }
}