def call(Map params) {
  
    // ✅ Set env vars properly
    env.GCP_PROJECT = params.gcpProject
    env.GITHUB_REPO = params.githubRepo
    env.ZONE = params.zone
    env.REGISTRY = params.registry
    env.CONSUL_IP = params.consulIP
    env.WORKSPACE = params.workspace

    def COMPOSE_FILE = 'docker-compose.yml'     
    
    stage('Build Order Service') {        

        echo "${env.WORKSPACE}/order-service/${COMPOSE_FILE}"
        sh "echo \"CONSUL_HOST=${env.CONSUL_IP}\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"CONSUL_PORT=8500\" >> ${env.WORKSPACE}/order-service/.env"

        sh "echo \"DB_USER=order_user\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"B_PASSWORD=order_password\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"CB_HOST=localhost\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"DB_PORT=5432\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"DB_NAME=order_db\" >> ${env.WORKSPACE}/order-service/.env"

        sh "echo \"SECRET_KEY=Something\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"API_KEY=Something\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"CALGORITHM=HS256\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"ACCESS_TOKEN_EXPIRE_MINUTES=30\" >> ${env.WORKSPACE}/order-service/.env"

        sh "echo \"CORS_ORIGINS=http://localhost:3000,http://127.0.0.1:3000\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"PROMETHEUS_ENABLED=true\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"LOG_LEVEL=INFO\" >> ${env.WORKSPACE}/order-service/.env"

        sh "cat ${env.WORKSPACE}/order-service/.env"

        sh "docker compose -f ${env.WORKSPACE}/order-service/${COMPOSE_FILE} up -d --build"
    }
    
    stage('Tag and Push Gateway Images') {
       
        def DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
        def HOME = "${env.DOCKER_HOME}"    
        
        def services = sh(
            script: "docker compose -f ${env.WORKSPACE}/order-service/${COMPOSE_FILE} config --services",
            returnStdout: true
        ).trim().split('\n')
                
        for (service in services) {
            echo "--- Processing service: ${service} ---"
        
            def sourceImage = sh(
                script: "docker compose -f ${env.WORKSPACE}/order-service/${COMPOSE_FILE} config | grep 'image:' | grep -A5 '${service}' | awk '{print \$2}'",
                returnStdout: true
            ).trim()

            def sourceImage1 = sh(script: "echo -e '${sourceImage}' | head -n 1", returnStdout: true).trim()

            
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
    
    stage('Cleanup order Containers') {
        echo "Stopping order containers"
        sh 'docker stop $(docker ps -q) || true'
    }
}

return this