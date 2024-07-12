def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhubpwd')
            SLACK_CREDENTIALS = credentials('slackpwd')
        }

        parameters {
            string(name: 'JAVA_REPO', defaultValue: 'https://github.com/pramilasawant/springboot1-application.git', description: 'Java Application Repository')
            string(name: 'PYTHON_REPO', defaultValue: 'https://github.com/pramilasawant/phython-application.git', description: 'Python Application Repository')
            string(name: 'DOCKERHUB_USERNAME', defaultValue: 'pramila188', description: 'DockerHub Username')
            string(name: 'JAVA_IMAGE_NAME', defaultValue: 'testhello', description: 'Java Docker Image Name')
            string(name: 'PYTHON_IMAGE_NAME', defaultValue: 'python-app', description: 'Python Docker Image Name')
            string(name: 'JAVA_NAMESPACE', defaultValue: 'test', description: 'Kubernetes Namespace for Java Application')
            string(name: 'PYTHON_NAMESPACE', defaultValue: 'python', description: 'Kubernetes Namespace for Python Application')
        }

        stages {
            stage('Clone Repositories') {
                steps {
                    parallel {
                        stage('Clone Java Repo') {
                            steps {
                                git url: params.JAVA_REPO
                            }
                        }
                        stage('Clone Python Repo') {
                            steps {
                                dir('python-app') {
                                    git url: params.PYTHON_REPO
                                }
                            }
                        }
                    }
                }
            }

            stage('Build and Push Docker Images') {
                steps {
                    parallel {
                        stage('Build and Push Java Image') {
                            steps {
                                dir('testhello')
                                script {
                                    sh 'mvn clean package'
                                    def image = docker.build("${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}")
                                    docker.withRegistry('', 'dockerhubpwd') {
                                        sh 'docker build -t pramila188/testhello:latest .'
                                        sh 'docker push pramila188/testhello:latest'
                                        image.push()
                                    }
                                }
                            }
                        }
                        stage('Build and Push Python Image') {
                            steps {
                                dir('python-app') {
                                    script {
                                        def image = docker.build("${params.DOCKERHUB_USERNAME}/${params.PYTHON_IMAGE_NAME}")
                                        docker.withRegistry('', 'dockerhubpwd') {
                                            image.push()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    parallel {
                        stage('Deploy Java Application') {
                            steps {
                                script {
                                    sh """
                                    kubectl create namespace ${params.JAVA_NAMESPACE} || true
                                    helm upgrade --install java-app helm/java-chart --namespace ${params.JAVA_NAMESPACE} \
                                    --set image.repository=${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}
                                    """
                                }
                            }
                        }
                        stage('Deploy Python Application') {
                            steps {
                                script {
                                    dir('python-app') {
                                        sh """
                                        kubectl create namespace ${params.PYTHON_NAMESPACE} || true
                                        helm upgrade --install python-app helm/python-chart --namespace ${params.PYTHON_NAMESPACE} \
                                        --set image.repository=${params.DOCKERHUB_USERNAME}/${params.PYTHON_IMAGE_NAME}
                                        """
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        post {
            success {
                slackSend channel: '#builds', color: 'good', message: "Build and Deployment of Java and Python applications succeeded."
            }
            failure {
                slackSend channel: '#builds', color: 'danger', message: "Build and Deployment of Java and Python applications failed."
            }
        }
    }
}
