pipeline {
    agent any

    stages {
        // --- PHASE 1 : TEST ---
        stage('Test') {
                    steps {
                        echo 'Lancement des tests unitaires...'
                        bat 'gradlew.bat test'
                    }
                    post {
                        always {
                            // 1. JUnit
                            junit 'build/test-results/test/*.xml'

                            // 2. CORRECTION CUCUMBER ICI : On a enlevé "buildId"
                            cucumber fileIncludePattern: '**/report.json',
                                     sortingMethod: 'ALPHABETICAL'
                        }
                    }
                }

        // --- PHASE 2 : CODE ANALYSIS ---
      //  stage('Code Analysis') {
            //steps {
              //  echo 'Analyse SonarQube...'
                // "sonar" doit correspondre au NOM que vous avez donné au serveur dans "Manage Jenkins -> System"
                //withSonarQubeEnv('sonar') {
                  //  bat 'gradlew.bat sonar'
                //}
            //}
        //}

        // --- PHASE 3 : CODE QUALITY ---
       // --- PHASE 3 : CODE QUALITY ---
               stage('Quality Gate') {
                   steps {
                       echo 'Vérification de la Quality Gate...'
                       // On augmente le temps d'attente à 5 minutes
                       timeout(time: 5, unit: 'MINUTES') {
                           waitForQualityGate abortPipeline: true
                       }
                   }
               }
        // --- PHASE 4 : BUILD ---
        stage('Build') {
            steps {
                echo 'Génération du JAR et de la Javadoc...'
                bat 'gradlew.bat assemble javadoc'
            }
            post {
                success {
                    // Archiver le JAR et la documentation dans Jenkins
                    archiveArtifacts artifacts: 'build/libs/*.jar, build/docs/javadoc/**/*', fingerprint: true
                }
            }
        }

        // --- PHASE 5 : DEPLOY ---
        stage('Deploy') {
            steps {
                echo 'Déploiement vers MyMavenRepo...'
                // Utilise les variables d''environnement configurées dans Jenkins (MYMAVENREPO_URL, etc.)
                bat 'gradlew.bat publish'
            }
        }
    }

    // --- PHASE 6 : NOTIFICATIONS (POST-BUILD) ---
    post {
        success {
            echo 'Pipeline réussi !'
            // 1. Envoi du mail de succès
            // Note: On utilise le plugin Email Extension ou Mailer configuré à l'étape 2
            emailext subject: "SUCCÈS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                     body: "Le build a réussi. Voir les détails : ${env.BUILD_URL}",
                     to: 'h_mokeddem@esi.dz' // Remplacez par l'email de l'équipe

            // 2. Notification Slack
            // Comme vous avez déjà créé la tâche "notifySlack" dans build.gradle, on l'appelle ici
            bat 'gradlew.bat notifySlack'
        }
        failure {
            echo 'Pipeline échoué !'
            // Envoi du mail d'échec
            emailext subject: "ÉCHEC: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                     body: "Le build a échoué. \nVeuillez vérifier la console : ${env.BUILD_URL}",
                     to: 'h_mokeddem@esi.dz'
        }
    }
}