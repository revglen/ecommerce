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
    env.SSH_KEY=params.ssh_key
    env.SSH_PUB_KEY=params.ssh_pub_key

    // ✅ Use 'def' for local vars to avoid Groovy warnings
    def GATEWAY_VM_NAME = 'api-gateway'
    def IMAGE_NAME = 'api-gateway'
    def COMPOSE_FILE = 'docker-compose.yml'    
    
    
    stage('Call Terraform and create a VM in GCP') {         

        sh 'terraform init'
        //sh 'terraform plan -out=tfplan'
        //sh 'terraform show tfplan'

        sh """
            terraform apply -auto-approve \
                -var="ssh_public_key=${env.SSH_PUB_KEY}" \
                -var="project_id=${env.TF_VAR_project_id}"
        """

        def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()
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
                                          
            // Save the Docker image as a tar file
            sh """
                docker save ${sourceImage} -o ${sourceImage}.tar
                echo "The docker saved to ${sourceImage}"
            """

            def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()

            // Copy the Docker image to the GCP VM
            sh """                
                scp -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ${sourceImage} ubuntu@${IP}:/home/ubuntu/
                echo "Copied to GCP VM"
            """
            
            // // SSH into the VM and load the Docker image
            sh """
                ssh -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ubuntu@${IP} 'docker load -i /home/ubuntu/${sourceImage}'
                echo "Loaded into the GCP VM"
            """   

            def imageName=${sourceImage}.split(':')[0]

            // Retry mechanism for SSH
            retry(3) {
                sh """
                    scp -o StrictHostKeyChecking=no \
                        -o ConnectTimeout=30 \
                        -i /var/lib/jenkins/.ssh/id_rsa \
                        ${imageName}.tar ubuntu@${IP}:/home/ubuntu/
                    echo "Completed the upload of the docker tar ball"
                """
            }
            
            // Execute remote commands
            sh """
                ssh -o StrictHostKeyChecking=no \
                    -i ${env.SSH_KEY} \
                    ubuntu@${IP} \
                    'docker load -i /home/${ubuntu}/${imageName} && \
                     docker run -d -p 80:80 my-app:latest'
            """
                              
        }
    }
    
    stage('Cleanup Gateway Containers') {
        echo "Stopping gateway containers"
        sh 'docker stop $(docker ps -q) || true'

        sh """
            rm -rf /var/lib/jenkins/.ssh
            rm -rf ${sourceImage}
            echo "Deleted the tar file"
        """   
    }
}

return this