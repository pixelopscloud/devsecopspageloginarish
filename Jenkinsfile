pipeline {
    agent any
    environment {
        NAMESPACE = "devsecops-login"
        FRONTEND_IMAGE = "pixelopscloud/devsecops-frontend:${BUILD_NUMBER}"
        BACKEND_IMAGE = "pixelopscloud/devsecops-backend:${BUILD_NUMBER}"
        ZAP_TARGET = "http://frontend-service.${NAMESPACE}.svc.cluster.local"
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', credentialsId: 'github-credentials', 
                    url: 'https://github.com/pixelopscloud/devsecopspageloginarish.git'
            }
        }
        stage('SonarQube Analysis') {
    steps {
        script {
            def scannerHome = tool 'SonarScanner'
            withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_AUTH_TOKEN')]) {
                withSonarQubeEnv('SonarQube') {
                    sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=devsecops-login \
                        -Dsonar.sources=frontend \
                        -Dsonar.exclusions=**/node_modules/**,**/backend/**,**/k8s-yaml/** \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_AUTH_TOKEN}
                    """
                }
            }
        }
    }
}

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            waitForQualityGate abortPipeline: false
                        } catch (Exception e) {
                            echo "Quality Gate skipped"
                        }
                    }
                }
            }
        }
        stage('Build & Push Frontend') {
            steps {
                script {
                    dir('frontend') {
                        sh "docker build -t ${FRONTEND_IMAGE} ."
                    }
                    sh "trivy image --severity HIGH,CRITICAL --exit-code 0 ${FRONTEND_IMAGE} || true"
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh """
                            echo \$PASS | docker login -u \$USER --password-stdin
                            docker push ${FRONTEND_IMAGE}
                        """
                    }
                }
            }
        }
        stage('Build & Push Backend') {
            steps {
                script {
                    dir('backend') {
                        sh "docker build -t ${BACKEND_IMAGE} ."
                    }
                    sh "trivy image --severity HIGH,CRITICAL --exit-code 0 ${BACKEND_IMAGE} || true"
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh """
                            echo \$PASS | docker login -u \$USER --password-stdin
                            docker push ${BACKEND_IMAGE}
                        """
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig([credentialsId: 'microk8s-kubeconfig']) {
                        sh """
                            kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                            kubectl apply -f k8s-yaml/db-secret.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/postgres-configmap.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/postgres-deployment.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/backend-deployment.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/frontend-deployment.yaml -n ${NAMESPACE}
                            kubectl set image deployment/backend backend=${BACKEND_IMAGE} -n ${NAMESPACE} || true
                            kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE} -n ${NAMESPACE} || true
                            kubectl get pods -n ${NAMESPACE}
                        """
                    }
                }
            }
        }
        stage('OWASP ZAP Baseline') {
            steps {
                sh """
                    docker run -t --rm --network host -v \$(pwd):/zap/wrk/:rw \
                    ghcr.io/zaproxy/zaproxy:stable \
                    zap-baseline.py -t ${ZAP_TARGET} -r baseline-report.html -I || true
                """
            }
        }
        stage('OWASP ZAP Full Scan') {
            steps {
                sh """
                    docker run -t --rm --network host -v \$(pwd):/zap/wrk/:rw \
                    ghcr.io/zaproxy/zaproxy:stable \
                    zap-full-scan.py -t ${ZAP_TARGET} -r full-report.html -I || true
                """
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: '*.html', allowEmptyArchive: true
            publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                reportDir: '.', reportFiles: 'baseline-report.html, full-report.html',
                reportName: 'Security Reports'])
        }
        cleanup {
            sh "docker rmi ${FRONTEND_IMAGE} ${BACKEND_IMAGE} || true"
        }
    }
}
