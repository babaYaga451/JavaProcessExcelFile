pipeline {
    agent any  // Runs on Jenkins agent (or Jenkins itself if no agents)

    parameters {
        string(name: 'INPUT_FILE', defaultValue: 'src/main/resources/shippers_large.xlsx', description: 'Path to input file inside the project')
    }

    environment {
        JAR_NAME = "target/ProcessExcelFile-0.0.1-SNAPSHOT.jar"
    }

    stages {
        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/babaYaga451/JavaProcessExcelFile.git'
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
}
