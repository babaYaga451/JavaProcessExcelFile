pipeline {
    agent any  // Runs on Jenkins agent (or Jenkins itself if no agents)

    tools {
        jdk 'jdk21'     // Use the JDK version installed in Jenkins UI
        maven 'maven'  // Use installed Maven
    }

    parameters {
        string(name: 'INPUT_FILE', defaultValue: 'src/main/resources/shippers_large.csv', description: 'Path to input file inside the project')
    }

    environment {
        JAR_NAME = "target/ProcessExcelFile-0.0.1-SNAPSHOT.jar"
        JAVA_HOME = '/opt/jdk-21'
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Test java version') {
                steps {
                    sh 'java -version'
                }
        }
        stage('Clone Repository') {
            steps {
                git branch: 'optimized-virtual-thread', url: 'https://github.com/babaYaga451/JavaProcessExcelFile.git'
            }
        }

        stage('Build Project') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Run Application') {
            steps {
                sh "java -jar -Dinput.file=${params.INPUT_FILE} ${JAR_NAME}"
            }
        }

        stage('Archive Output Files') {
            steps {
                archiveArtifacts artifacts: 'target/output/**', fingerprint: true
            }
        }
    }

    post {
        always {
            sh 'rm -rf ~/.m2/repository'
            echo '🗑️ Maven cache deleted after build!'
        }
        success {
            echo '✅ Build and Execution Successful!'
        }
        failure {
            echo '❌ Build Failed!'
        }
    }
}
