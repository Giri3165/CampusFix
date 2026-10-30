pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    triggers {
        githubPush()
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'MAVEN_PROFILE', choices: ['ci', 'fast'],
               description: 'ci = run tests, fast = skip tests')
        booleanParam(name: 'DEPLOY', defaultValue: false,
                     description: 'Deploy to Azure VM')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B -ntp clean compile'
            }
        }

        stage('Test') {
            steps {
                bat "mvn -B -ntp test -P${params.MAVEN_PROFILE}"
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'

                    publishHTML(target: [
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage',
                        keepAll: true,
                        alwaysLinkToLastBuild: true,
                        allowMissing: true
                    ])
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B -ntp package -DskipTests'
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/campusfix.jar',
                                 fingerprint: true
            }
        }

        stage('Deploy to Azure VM') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                echo 'Azure deployment requires a Linux agent with Ansible configured.'
                error 'Deployment not configured on this Windows Jenkins agent.'
            }
        }
    }

    post {
        success {
            echo 'CampusFix build finished successfully.'
        }
        failure {
            echo 'CampusFix build failed - check the stage logs above.'
        }
        cleanup {
            bat 'if exist ansible\\inventory.generated del ansible\\inventory.generated'
        }
    }
}