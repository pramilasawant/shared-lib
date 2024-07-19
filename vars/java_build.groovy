

pipeline {
    agent any
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhubpwd')
        SLACK_CREDENTIALS = credentials('slack-token-id')
    }
    stages {
        stage('Cleanup Workspace') {
            steps {
                deleteDir()
            }
        }
        stage('Checkout Repositories') {
            parallel {
                stage('Checkout Java Application') {
                    steps {
                        dir('java-app') {
                            git branch: 'main', url: 'https://github.com/pramilasawant/java-app.git'
                        }
                    }
                }
                stage('Checkout Python Application') {
                    steps {
                        dir('python-app') {
                            git branch: 'main', url: 'https://github.com/pramilasawant/phython-application.git'
                        }
                    }
                }
            }
        }
        stage('Build and Deploy') {
            parallel {
                stage('Build and Push Java Application') {
                    steps {
                        script {
                            dir('java-app') {
                                sh 'ls -l' // Check files
                                withDockerRegistry([url: '', credentialsId: 'dockerhubpwd']) {
                                    sh 'docker build -t pramila188/java-app .'
                                    sh 'docker tag pramila188/java-app:latest index.docker.io/pramila188/java-app:latest'
                                    sh 'docker push index.docker.io/pramila188/java-app:latest'
                                }
                                sh './mvnw clean package'
                            }
                        }
                    }
                }
                stage('Build and Push Python Application') {
                    steps {
                        script {
                            dir('python-app') {
                                sh 'ls -l' // Check files
                                withDockerRegistry([url: '', credentialsId: 'dockerhubpwd']) {
                                    sh 'docker build -t pramila188/python-app:latest .'
                                    sh 'docker tag pramila188/python-app:latest index.docker.io/pramila188/python-app:latest'
                                    sh 'docker push index.docker.io/pramila188/python-app:latest'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            cleanWs()
            slackSend(channel: '#build-status', color: '#FF0000', message: "Build failed: ${env.JOB_NAME} #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)", tokenCredentialId: 'slack-token-id')
        }
    }
}

