def call(Map config = [:]) {
    pipeline {
        agent any
        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhub_credentials_id')
            DOCKERHUB_USERNAME = "${DOCKERHUB_CREDENTIALS_USR}"
            DOCKERHUB_PASSWORD = "${DOCKERHUB_CREDENTIALS_PSW}"
        }
        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }
            stage('Build') {
                steps {
                    script {
                        sh ' sudo mvn clean install'
                    }
                }
            }
            stage('Build Docker Image') {
                steps {
                    script {
                        sh 'docker build -t ${DOCKERHUB_USERNAME}/${config.imageName}:latest .'
                    }
                }
            }
            stage('Push Docker Image') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: 'dockerhub_credentials_id', url: 'https://index.docker.io/v1/']) {
                            sh 'docker push ${DOCKERHUB_USERNAME}/${config.imageName}:latest'
                        }
                    }
                }
            }
            stage('Cleanup') {
                steps {
                    cleanWs()
                }
            }
        }
        post {
            always {
                script {
                    if (currentBuild.result != 'SUCCESS') {
                        slackSend(
                            channel: '#your-channel',
                            color: '#FF0000',
                            message: "Build failed: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
                        )
                    } else {
                        slackSend(
                            channel: '#your-channel',
                            color: '#00FF00',
                            message: "Build succeeded: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
                        )
                    }
                }
            }
        }
    }
}
