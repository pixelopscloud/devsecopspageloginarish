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
                git branch: 'main', 
                    credentialsId: 'github-credentials', 
                    url: 'https://github.com/pixelopscloud/devsecopspageloginarish.git'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarScanner'
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=devsecops-login \
                            -Dsonar.sources=. \
                            -Dsonar.host.url=${env.SONAR_HOST_URL} \
                            -Dsonar.login=${env.SONAR_AUTH_TOKEN}
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build & Push Frontend') {
            steps {
                script {
                    dir('frontend') {
                        sh "docker build -t ${FRONTEND_IMAGE} ."
                    }
                    
                    // Trivy scan with proper error handling
                    sh """
                        trivy image --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        ${FRONTEND_IMAGE} || echo 'Trivy scan completed with warnings'
                    """
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
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
                    
                    // Trivy scan with proper error handling
                    sh """
                        trivy image --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        ${BACKEND_IMAGE} || echo 'Trivy scan completed with warnings'
                    """
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
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
                            # Create namespace if not exists
                            kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                            # Apply all YAMLs in order
                            kubectl apply -f k8s-yaml/db-secret.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/postgres-configmap.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/postgres-deployment.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/backend-deployment.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/frontend-deployment.yaml -n ${NAMESPACE}

                            # Update images with new build tags
                            kubectl set image deployment/backend backend=${BACKEND_IMAGE} -n ${NAMESPACE}
                            kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE} -n ${NAMESPACE}

                            # Wait for deployments to be ready
                            kubectl rollout status deployment/backend -n ${NAMESPACE} --timeout=5m
                            kubectl rollout status deployment/frontend -n ${NAMESPACE} --timeout=5m

                            # Show deployment status
                            kubectl get pods -n ${NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('OWASP ZAP Baseline Scan') {
            steps {
                script {
                    sh """
                        docker run -t --rm \
                          --network host \
                          -v \$(pwd):/zap/wrk/:rw \
                          ghcr.io/zaproxy/zaproxy:stable \
                          zap-baseline.py -t ${ZAP_TARGET} \
                          -r baseline-report.html \
                          -I \
                          || echo 'ZAP Baseline scan completed with findings'
                    """
                }
            }
        }

        stage('OWASP ZAP Full Scan') {
            steps {
                script {
                    sh """
                        docker run -t --rm \
                          --network host \
                          -v \$(pwd):/zap/wrk/:rw \
                          ghcr.io/zaproxy/zaproxy:stable \
                          zap-full-scan.py -t ${ZAP_TARGET} \
                          -r full-report.html \
                          -I \
                          || echo 'ZAP Full scan completed with findings'
                    """
                }
            }
        }
    }

    post {
        always {
            // Archive reports if they exist
            archiveArtifacts artifacts: '*.html', allowEmptyArchive: true
            
            // Publish HTML reports
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'baseline-report.html, full-report.html',
                reportName: 'OWASP ZAP Security Reports'
            ])
        }
        
        success {
            echo '✅ Pipeline completed successfully!'
            echo "Frontend Image: ${FRONTEND_IMAGE}"
            echo "Backend Image: ${BACKEND_IMAGE}"
        }
        
        failure {
            echo '❌ Pipeline failed! Check logs for details.'
        }
        
        cleanup {
            // Clean up local Docker images to save space
            sh """
                docker rmi ${FRONTEND_IMAGE} ${BACKEND_IMAGE} || true
                docker system prune -f || true
            """
        }
    }
}
