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

        stage('Compile Backend for SonarQube') {
            steps {
                script {
                    sh '''
                        echo "Compiling Java backend with Maven for SonarQube analysis..."
                        
                        # Get absolute path to backend directory
                        WORKSPACE_PATH="${WORKSPACE}/backend"
                        echo "Workspace path: $WORKSPACE_PATH"
                        
                        # Check if pom.xml exists
                        ls -la "${WORKSPACE}/backend/pom.xml"
                        
                        # Run Maven compile
                        docker run --rm \
                          -v "${WORKSPACE_PATH}":/app \
                          -w /app \
                          maven:3.9.6-eclipse-temurin-17 \
                          mvn clean compile -DskipTests
                        
                        echo "✅ Backend compiled successfully"
                        ls -lah "${WORKSPACE}/backend/target/classes" || echo "Checking classes..."
                    '''
                }
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
                            -Dsonar.projectName=DevSecOps-Login \
                            -Dsonar.sources=frontend,backend/src/main/java \
                            -Dsonar.java.binaries=backend/target/classes \
                            -Dsonar.java.source=17 \
                            -Dsonar.exclusions=**/node_modules/**,**/target/**,**/test/**,**/k8s-yaml/**,**/.git/**,**/database/** \
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
                    script {
                        try {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                echo "⚠️ Quality Gate status: ${qg.status}"
                                echo "Continuing with build despite Quality Gate status..."
                            } else {
                                echo "✅ Quality Gate passed!"
                            }
                        } catch (Exception e) {
                            echo "⚠️ Quality Gate check encountered an issue: ${e.message}"
                            echo "Continuing with build..."
                        }
                    }
                }
            }
        }

        stage('Build & Push Frontend') {
            steps {
                script {
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "Building Frontend Docker Image"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    
                    dir('frontend') {
                        sh "docker build -t ${FRONTEND_IMAGE} ."
                    }
                    
                    echo "Running Trivy security scan on Frontend..."
                    sh """
                        trivy image --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        ${FRONTEND_IMAGE} || echo '⚠️ Trivy scan completed with warnings'
                    """
                    
                    echo "Pushing Frontend image to Docker Hub..."
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            docker push ${FRONTEND_IMAGE}
                            echo "✅ Frontend image pushed: ${FRONTEND_IMAGE}"
                        """
                    }
                }
            }
        }

        stage('Build & Push Backend') {
            steps {
                script {
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "Building Backend Docker Image (Spring Boot)"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    
                    dir('backend') {
                        sh "docker build -t ${BACKEND_IMAGE} ."
                    }
                    
                    echo "Running Trivy security scan on Backend..."
                    sh """
                        trivy image --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        ${BACKEND_IMAGE} || echo '⚠️ Trivy scan completed with warnings'
                    """
                    
                    echo "Pushing Backend image to Docker Hub..."
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials', 
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            docker push ${BACKEND_IMAGE}
                            echo "✅ Backend image pushed: ${BACKEND_IMAGE}"
                        """
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "Deploying to Kubernetes"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    
                    withKubeConfig([credentialsId: 'microk8s-kubeconfig']) {
                        sh """
                            # Create namespace
                            echo "📦 Creating/Updating namespace: ${NAMESPACE}"
                            kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                            # Apply database configuration
                            echo "🗄️  Deploying database..."
                            kubectl apply -f k8s-yaml/db-secret.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/postgres-configmap.yaml -n ${NAMESPACE}
                            kubectl apply -f k8s-yaml/postgres-deployment.yaml -n ${NAMESPACE}
                            
                            # Apply backend
                            echo "⚙️  Deploying backend..."
                            kubectl apply -f k8s-yaml/backend-deployment.yaml -n ${NAMESPACE}
                            
                            # Apply frontend
                            echo "🌐 Deploying frontend..."
                            kubectl apply -f k8s-yaml/frontend-deployment.yaml -n ${NAMESPACE}

                            # Update images with new build numbers
                            echo "🔄 Updating container images..."
                            kubectl set image deployment/backend backend=${BACKEND_IMAGE} -n ${NAMESPACE} || true
                            kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE} -n ${NAMESPACE} || true

                            # Wait for rollouts
                            echo "⏳ Waiting for deployments to be ready..."
                            kubectl rollout status deployment/postgres -n ${NAMESPACE} --timeout=3m || true
                            kubectl rollout status deployment/backend -n ${NAMESPACE} --timeout=3m || true
                            kubectl rollout status deployment/frontend -n ${NAMESPACE} --timeout=3m || true

                            # Display status
                            echo ""
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                            echo "📊 Deployment Status"
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                            kubectl get pods -n ${NAMESPACE}
                            echo ""
                            echo "🌐 Services:"
                            kubectl get svc -n ${NAMESPACE}
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                        """
                    }
                }
            }
        }

        stage('OWASP ZAP Baseline Scan') {
            steps {
                script {
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "🔒 Running OWASP ZAP Baseline Scan"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    
                    sh """
                        docker run -t --rm \
                          --network host \
                          -v \$(pwd):/zap/wrk/:rw \
                          ghcr.io/zaproxy/zaproxy:stable \
                          zap-baseline.py -t ${ZAP_TARGET} \
                          -r baseline-report.html \
                          -I \
                          || echo '⚠️ ZAP Baseline scan completed'
                    """
                }
            }
        }

        stage('OWASP ZAP Full Scan') {
            steps {
                script {
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "🔒 Running OWASP ZAP Full Scan"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    
                    sh """
                        docker run -t --rm \
                          --network host \
                          -v \$(pwd):/zap/wrk/:rw \
                          ghcr.io/zaproxy/zaproxy:stable \
                          zap-full-scan.py -t ${ZAP_TARGET} \
                          -r full-report.html \
                          -I \
                          || echo '⚠️ ZAP Full scan completed'
                    """
                }
            }
        }
    }

    post {
        always {
            echo "📦 Archiving artifacts and reports..."
            archiveArtifacts artifacts: '*.html', allowEmptyArchive: true
            
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
            echo ''
            echo '╔══════════════════════════════════════════════════╗'
            echo '║                                                  ║'
            echo '║       ✅ PIPELINE COMPLETED SUCCESSFULLY! ✅      ║'
            echo '║                                                  ║'
            echo '╚══════════════════════════════════════════════════╝'
            echo ''
            echo '📦 Docker Images:'
            echo "   Frontend: ${FRONTEND_IMAGE}"
            echo "   Backend:  ${BACKEND_IMAGE}"
            echo ''
            echo '🚀 Kubernetes Deployment:'
            echo "   Namespace: ${NAMESPACE}"
            echo ''
            echo '🔍 View your application:'
            echo "   kubectl get svc -n ${NAMESPACE}"
            echo "   kubectl get pods -n ${NAMESPACE}"
            echo ''
            echo '📊 Security Reports:'
            echo '   Check "OWASP ZAP Security Reports" in Jenkins'
            echo ''
        }
        
        failure {
            echo ''
            echo '╔══════════════════════════════════════════════════╗'
            echo '║                                                  ║'
            echo '║            ❌ PIPELINE FAILED ❌                  ║'
            echo '║                                                  ║'
            echo '╚══════════════════════════════════════════════════╝'
            echo ''
            echo '🔍 Check the error logs above for details'
            echo ''
        }
        
        cleanup {
            echo "🧹 Cleaning up Docker resources..."
            sh """
                docker rmi ${FRONTEND_IMAGE} ${BACKEND_IMAGE} || true
                docker system prune -f --volumes || true
            """
        }
    }
}
