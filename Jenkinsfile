// CampusFix CI/CD pipeline for Jenkins (Unit III).
// Required: Global Tool Configuration -> JDK named "JDK17" and Maven named "Maven3".
// Required plugins: Git, Pipeline, Maven Integration, HTML Publisher, Credentials Binding, JUnit.
pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    triggers {
        githubPush()          // build on every push (needs the GitHub webhook, see docs/DEVOPS_GUIDE.md)
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'MAVEN_PROFILE', choices: ['ci', 'fast'], description: 'ci = run tests with JaCoCo coverage, fast = skip tests')
        booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Deploy to the Azure VM with Ansible after a successful build')
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build') {
            steps { sh 'mvn -B -ntp clean compile' }
        }

        stage('Test') {
            steps { sh "mvn -B -ntp test -P${params.MAVEN_PROFILE}" }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    publishHTML(target: [
                        reportDir: 'target/site/jacoco', reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage', keepAll: true,
                        alwaysLinkToLastBuild: true, allowMissing: true])
                }
            }
        }

        stage('Package') {
            steps { sh 'mvn -B -ntp package -DskipTests' }
        }

        stage('Archive') {
            steps { archiveArtifacts artifacts: 'target/campusfix.jar', fingerprint: true }
        }

        stage('Deploy to Azure VM') {
            when { expression { params.DEPLOY } }
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'azure-vm-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
                    string(credentialsId: 'campusfix-db-password', variable: 'DB_PASSWORD'),
                    string(credentialsId: 'campusfix-admin-password', variable: 'ADMIN_PASSWORD'),
                    string(credentialsId: 'azure-vm-host', variable: 'VM_HOST')
                ]) {
                    sh '''
                        printf "[campusfix]\\n%s ansible_user=%s ansible_ssh_private_key_file=%s\\n" "$VM_HOST" "$SSH_USER" "$SSH_KEY" > ansible/inventory.generated
                        export ANSIBLE_HOST_KEY_CHECKING=False
                        ansible-galaxy collection install -r ansible/requirements.yml
                        ansible-playbook -i ansible/inventory.generated ansible/deploy.yml -e jar_src="$WORKSPACE/target/campusfix.jar"
                    '''
                }
            }
        }
    }

    post {
        success { echo 'CampusFix build finished successfully.' }
        failure { echo 'CampusFix build failed - check the stage logs above.' }
        cleanup { sh 'rm -f ansible/inventory.generated' }
    }
}
