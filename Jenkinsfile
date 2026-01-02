pipeline {
    agent any

    environment {
        NAMESPACE = "devsecops-login"
        FRONTEND_IMAGE = "pixelopscloud/devsecops-frontend:${BUILD_NUMBER}"
        BACKEND_IMAGE = "pixelopscloud/devsecops-backend:${BUILD_NUMBER}"
        ZAP_TARGET = "http://frontend-service.${NAMESPACE}.svc.cluster.local"  // Internal for scan
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', credentialsId: 'github-cred', url: 'https://github.com/pixelopscloud/devsecopspageloginarish.git'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'sonar-scanner -Dsonar.projectKey=devsecops-login -Dsonar.sources=.'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') { waitForQualityGate abortPipeline: true }
            }
        }

        stage('Build & Push Frontend') {
            steps {
                dir('frontend') {
                    sh "docker build -t ${FRONTEND_IMAGE} ."
                }
                sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${FRONTEND_IMAGE}"
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                    sh "docker push ${FRONTEND_IMAGE}"
                }
            }
        }

        stage('Build & Push Backend') {
            steps {
                dir('backend') {
                    sh "docker build -t ${BACKEND_IMAGE} ."
                }
                sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${BACKEND_IMAGE}"
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                    sh "docker push ${BACKEND_IMAGE}"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withKubeConfig([credentialsId: 'kube-config']) {
                    sh '''
                    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # Apply all YAMLs
                    kubectl apply -f k8s-yaml/db-secret.yaml
                    kubectl apply -f k8s-yaml/postgres-configmap.yaml
                    kubectl apply -f k8s-yaml/postgres-deployment.yaml
                    kubectl apply -f k8s-yaml/backend-deployment.yaml
                    kubectl apply -f k8s-yaml/frontend-deployment.yaml

                    # Update images with new tags
                    kubectl set image deployment/backend backend=${BACKEND_IMAGE} -n ${NAMESPACE}
                    kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE} -n ${NAMESPACE}
                    '''
                }
            }
        }

        stage('OWASP ZAP Baseline Scan') {
            steps {
                sh '''
                docker run -t --network host \
                  -v $(pwd):/zap/wrk/:rw \
                  ghcr.io/zaproxy/zaproxy:weekly \
                  zap-baseline.py -t ${ZAP_TARGET} -r baseline-report.html --autooff
                '''
            }
        }

        stage('OWASP ZAP Full Scan') {
            steps {
                sh '''
                docker run -t --network host \
                  -v $(pwd):/zap/wrk/:rw \
                  ghcr.io/zaproxy/zaproxy:weekly \
                  zap-full-scan.py -t ${ZAP_TARGET} -r full-report.html
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '*.html', allowEmptyArchive: true
        }
        cleanup {
            sh "docker rmi ${FRONTEND_IMAGE} ${BACKEND_IMAGE} || true"
        }
    }
}
