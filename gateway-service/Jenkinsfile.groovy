def call(Map params) {
  
    // ✅ Set env vars properly
    env.GCP_PROJECT = params.gcpProject
    env.GITHUB_REPO = params.githubRepo
    env.ZONE = params.zone
    env.REGISTRY = params.registry
    env.CONSUL_IP = params.consulIP
    env.WORKSPACE = params.workspace
    env.GOOGLE_CREDENTIALS = params.google_credentials
    env.TF_VAR_project_id = params.gcpProject

    // ✅ Use 'def' for local vars to avoid Groovy warnings
    def GATEWAY_VM_NAME = 'api-gateway'
    def IMAGE_NAME = 'api-gateway'
    def COMPOSE_FILE = 'docker-compose.yml'    
    
    
    stage('Call Terraform and create a VM in GCP') { 
        /*sh 'terraform init'
        sh 'terraform plan -out=tfplan'
        sh 'terraform show tfplan'*/

        sh 'terraform apply -auto-approve'
        //def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()
        def IP = "1.2.3.4"
        sh "echo \"CONSUL_IP=${IP}\" > ${env.WORKSPACE}/gateway-service/.env"
        sh "cat ${env.WORKSPACE}/gateway-service/.env"
        sh "docker compose -f ${env.WORKSPACE}/gateway-service/${COMPOSE_FILE} up -d --build"
    }
    
    stage('Tag and Push Gateway Images') {
       
        def DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
        def HOME = "${env.DOCKER_HOME}"    
        
        def services = sh(
            script: "docker compose -f ${env.WORKSPACE}/gateway-service/${COMPOSE_FILE} config --services",
            returnStdout: true
        ).trim().split('\n')
                
        for (service in services) {
            echo "--- Processing service: ${service} ---"
        
            def sourceImage = sh(
                script: "docker compose -f ${env.WORKSPACE}/gateway-service/${COMPOSE_FILE} config | grep -A15 '${service}:' | grep 'image:' | awk '{print \$2}'",
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
                               
            // Save the Docker image as a tar file
            sh """
                docker save ${targetImage} -o ${targetImage}.tar'
            """

            // Copy the Docker image to the GCP VM
            // sh """
            //     scp -o StrictHostKeyChecking=no -i ~/.ssh/id_rsa ${targetImage}.tar ubuntu@${env.INSTANCE_IP}:/home/ubuntu/
            // """
            
            // // SSH into the VM and load the Docker image
            // sh """
            //     ssh -o StrictHostKeyChecking=no -i ~/.ssh/id_rsa ubuntu@${env.INSTANCE_IP} 'docker load -i /home/ubuntu/${targetImage}.tar'
            // """                    
        }
    }
    
    stage('Cleanup Gateway Containers') {
        echo "Stopping gateway containers"
        sh 'docker stop $(docker ps -q) || true'
    }
}

return this