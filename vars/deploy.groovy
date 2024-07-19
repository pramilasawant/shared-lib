pipeline {
    agent any

    environment {
        KUBECONFIG = credentials('kubeconfig-id') // Replace with your Kubernetes credentials ID
        HELM_CHART_PATH = 'helm-charts'
    }

    stages {
        stage('Deploy Java Application') {
            steps {
                script {
                    // Deploy Java application
                    dir('testhello') {
                        sh 'helm upgrade --install java-app ${HELM_CHART_PATH}/java-chart --namespace test --set image.repository=pramila188/testhello --set image.tag=latest'
                    }
                }
            }
        }
        
        stage('Deploy Python Application') {
            steps {
                script {
                    // Deploy Python application
                    dir('python-app') {
                        sh 'helm upgrade --install python-app ${HELM_CHART_PATH}/python-chart --namespace python --set image.repository=pramila188/python-app --set image.tag=latest'
                    }
                }
            }
        }
    }

    post {
        success {
            slackSend(channel: '#build-status', color: 'good', message: "Deployment succeeded for ${env.JOB_NAME} #${env.BUILD_NUMBER}")
        }
        failure {
            slackSend(channel: '#build-status', color: 'danger', message: "Deployment failed for ${env.JOB_NAME} #${env.BUILD_NUMBER}")
        }
    }
}
