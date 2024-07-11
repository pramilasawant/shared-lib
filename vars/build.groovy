def call(Map config) {
    pipeline {
        agent any
        environment {
            DOCKERHUB_CREDENTIALS = ('dockerhubpwd')
            DOCKERHUB_USERNAME = 'pramila188'
        }
        stages {
            stage('Clone Repositories') {
                parallel {
                    stage('Clone Java Repo') {
                        steps {
                            git url: 'https://github.com/pramilasawant/springboot1-application.git', branch: 'main'
                        }
                    }
                    stage('Clone Python Repo') {
                        steps {
                            git url: 'https://github.com/pramilasawant/phython-application.git', branch: 'main'
                        }
                    }
                }
            }
            stage('Build Docker Images') {
                parallel {
                    stage('Build Java Docker Image') {
                        steps {
                            script {
                                def javaImage = docker.build("${env.DOCKERHUB_USERNAME}/testhello")
                                docker.withRegistry('', env.DOCKERHUB_CREDENTIALS) {
                                    javaImage.push('latest')
                                }
                            }
                        }
                    }
                    stage('Build Python Docker Image') {
                        steps {
                            script {
                                def pythonImage = docker.build("${env.DOCKERHUB_USERNAME}/python-app")
                                docker.withRegistry('', env.DOCKERHUB_CREDENTIALS) {
                                    pythonImage.push('latest')
                                }
                            }
                        }
                    }
                }
            }
            stage('Deploy to Kubernetes') {
                steps {
                    script {
                        kubernetesDeploy(configs: 'deploymentservice.yaml', kubeconfigId: 'k8sconfigpwd')
                    }
                }
            }
        }
        post {
            success {
                slackSend(channel: '#build-notifications', message: "Build and Deployment Successful: ${env.BUILD_URL}")
            }
            failure {
                slackSend(channel: '#build-notifications', message: "Build and Deployment Failed: ${env.BUILD_URL}")
            }
        }
    }
}
