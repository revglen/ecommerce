def call(Map params) {
  
    // ✅ Set env vars properly
    env.GCP_PROJECT = params.gcpProject
    env.GITHUB_REPO = params.githubRepo
    env.ZONE = params.zone
    env.REGISTRY = params.registry
    env.CONSUL_IP = params.consulIP
    env.WORKSPACE = params.workspace

    def COMPOSE_FILE = 'docker-compose.yml'
    def CONSUL_IP = sh(
            script: "hostname -I | awk '{print \$1}'",
            returnStdout: true
        ).trim()
    
    stage('Build observability Service') {        

        echo "${env.WORKSPACE}/observability//${COMPOSE_FILE}"
        sh "echo \"CONSUL_HOST=${env.CONSUL_IP}\" >> ${env.WORKSPACE}/observability/.env"
        sh "echo \"CONSUL_PORT=8500\" >> ${env.WORKSPACE}/observability/.env"
        sh "echo \"PROMETHEUS_ENABLED=true\" >> ${env.WORKSPACE}/observability/.env"
        sh "echo \"LOG_LEVEL=INFO\" >> ${env.WORKSPACE}/observability/.env"
        sh "cat ${env.WORKSPACE}/observability//.env"
        sh "docker compose -f ${env.WORKSPACE}/observability/${COMPOSE_FILE} up -d --build"
    }
    
    stage('Tag and Push Gateway Images') {
       
        def DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
        def HOME = "${env.DOCKER_HOME}"    
        
        def services = sh(
            script: "docker compose -f ${env.WORKSPACE}/observability//${COMPOSE_FILE} config --services",
            returnStdout: true
        ).trim().split('\n')
                
        for (service in services) {
            echo "--- Processing service: ${service} ---"
        
            def sourceImage = sh(
                script: "docker compose -f ${env.WORKSPACE}/observability/${COMPOSE_FILE} config | grep -A5 '${service}' | grep 'image:' | awk '{print \$2}'",
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
            
            //sh """
            //docker push ${targetImage}
            //"""
        }
    }
    
    stage('Cleanup observability/ Containers') {
        echo "Stopping observability/ containers"
        sh 'docker stop $(docker ps -q) || true'
    }
}

return this