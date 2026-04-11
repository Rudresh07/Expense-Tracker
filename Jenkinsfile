pipeline {
    agent any

    environment {
        ANDROID_HOME = "/Users/cepl/Library/Android/sdk"
        GRADLE_OPTS = "-Dorg.gradle.daemon=false"
    }

    stages {

        stage('Checkout') {
            steps {
                git 'https://github.com/Rudresh07/Expense-Tracker.git'
            }
        }

        stage('Clean') {
            steps {
                sh './gradlew clean'
            }
        }

        stage('Build Debug APK') {
            steps {
                sh './gradlew assembleDebug'
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh './gradlew testDebugUnitTest'
            }
        }

        stage('Lint Check') {
            steps {
                sh './gradlew lintDebug'
            }
        }

        stage('Build Release APK') {
            steps {
                sh './gradlew assembleRelease'
            }
        }

        stage('Archive APK') {
            steps {
                archiveArtifacts artifacts: '**/build/outputs/**/*.apk', fingerprint: true
            }
        }
    }

    post {
        success {
            echo 'Build Successful!'
        }
        failure {
            echo 'Build Failed!'
        }
    }
}
