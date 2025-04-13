def call(Map params) {
  
    // ✅ Set env vars properly
    env.GCP_PROJECT = params.gcpProject
    env.GITHUB_REPO = params.githubRepo
    env.ZONE = params.zone
    env.REGISTRY = params.registry
    env.CONSUL_IP = params.consulIP
    env.WORKSPACE = params.workspace

    // ✅ Use 'def' for local vars to avoid Groovy warnings
    def GATEWAY_VM_NAME = 'api-gateway'
    def IMAGE_NAME = 'api-gateway'
    def COMPOSE_FILE = 'docker-compose.yml'
    
    
    
    stage('Build Gateway Service') {
        steps {
            echo "${env.WORKSPACE}/gateway-service/${env.COMPOSE_FILE}"
            sh "echo \"CONSUL_IP=${env.CONSUL_IP}\" >> .env"
            sh "cat .env"
            sh "docker compose -f ${env.COMPOSE_FILE} up -d --build"
        }
    }
    
    stage('Tag and Push Gateway Images') {
        environment {
            DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
            HOME = "${env.DOCKER_HOME}"
        }
        steps {
            script {
                def services = sh(
                    script: "docker compose -f ${env.COMPOSE_FILE} config --services",
                    returnStdout: true
                ).trim().split('\n')
                
                services.each { service ->
                    echo "--- Processing service: ${service} ---"
                
                    def sourceImage = sh(
                        script: "docker compose -f ${env.COMPOSE_FILE} config | grep -A15 '${service}:' | grep 'image:' | awk '{print \$2}'",
                        returnStdout: true
                    ).trim()
                    
                    echo "Source image from compose: '${sourceImage}'"
                    
                    def imageNameOnly = sourceImage.split('/').last()
                    def targetImage = "${REGISTRY}/${imageNameOnly}-${env.BUILD_NUMBER}"
                    
                    echo "Calculated target image: '${targetImage}'"
                    
                    def imageId = sh(
                        script: "docker images -q ${sourceImage}",
                        returnStdout: true
                    ).trim()
                    
                    if (!imageId) {
                        echo "WARNING: Source image not found! Listing available images:"
                        sh 'docker images'
                        error "Source image ${sourceImage} not found in local registry!"
                    }
                    
                    sh """
                    docker tag ${sourceImage} ${targetImage}
                    """
                    
                    /*
                    sh """
                    docker push ${targetImage}
                    """
                    */
                }
            }
        }
    }
    
    stage('Cleanup Gateway Containers') {
        steps {
            echo "Stopping gateway containers"
            sh 'docker stop $(docker ps -q) || true'
        }
    }
}

return this