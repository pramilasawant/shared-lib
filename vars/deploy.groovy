def call(Map params = [:]) {
    pipeline {
        agent any
        
        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhub_credentials')
            SLACK_CREDENTIALS = credentials('slack_credentials')
            JAVA_IMAGE = 'pramila188/testhello'
            PYTHON_IMAGE = 'pramila188/python-app'
            JAVA_HELM_CHART = 'myspringbootchart'
            PYTHON_HELM_CHART = 'python-app'
            JAVA_NAMESPACE = 'test'
            PYTHON_NAMESPACE = 'python'
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
                            dir('python-app') {
                                git url: 'https://github.com/pramilasawant/phython-application.git', branch: 'main'
                            }
                        }
                    }
                }
            }
            
            stage('Build and Push Docker Images') {
                parallel {
                    stage('Build and Push Java Image') {
                        steps {
                            script {
                                docker.build("${JAVA_IMAGE}").push('latest')
                            }
                        }
                    }
                    stage('Build and Push Python Image') {
                        steps {
                            dir('python-app') {
                                script {
                                    docker.build("${PYTHON_IMAGE}").push('latest')
                                }
                            }
                        }
                    }
                }
            }
            
            stage('Deploy with Helm') {
                steps {
                    script {
                        // Deploy Java application
                        sh """
                        helm upgrade --install ${JAVA_HELM_CHART} ./helm/${JAVA_HELM_CHART} \
                            --namespace ${JAVA_NAMESPACE} \
                            --set image.repository=${JAVA_IMAGE} \
                            --set image.tag=latest
                        """
                        // Deploy Python application
                        dir('python-app') {
                            sh """
                            helm upgrade --install ${PYTHON_HELM_CHART} ./helm/${PYTHON_HELM_CHART} \
                                --namespace ${PYTHON_NAMESPACE} \
                                --set image.repository=${PYTHON_IMAGE} \
                                --set image.tag=latest
                            """
                        }
                    }
                }
            }
        }
        
        post {
            success {
                slackSend (
                    channel: '#jenkins',
                    color: 'good',
                    message: "Build and deployment succeeded: ${env.BUILD_URL}"
                )
            }
            failure {
                slackSend (
                    channel: '#jenkins',
                    color: 'danger',
                    message: "Build and deployment failed: ${env.BUILD_URL}"
                )
            }
        }
    }
}
