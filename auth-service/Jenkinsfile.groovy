def call(Map params) {
  
    // ✅ Set env vars properly
    env.GCP_PROJECT = params.gcpProject
    env.GITHUB_REPO = params.githubRepo
    env.ZONE = params.zone
    env.WORKSPACE = params.workspace
    env.GOOGLE_CREDENTIALS = params.google_credentials
    env.TF_VAR_project_id = params.gcpProject
    env.SSH_KEY=params.ssh_key
    env.SSH_PUB_KEY=params.ssh_pub_key
    env.PORTS=params.ports
    env.CONSUL_IP=params.consul_ip
    env.AUTHENICATION_PORTS=params.ports

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
        sh "ssh-keygen -R ${IP} || true"
        sh "echo \"CONSUL_IP=${env.CONSUL_IP}\" > ${env.WORKSPACE}/gateway-service/.env"
        sh "echo \"CONSUL_PORT=8500\" >> ${env.WORKSPACE}/auth-service/.env"

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
                docker save ${sourceImage} -o ${service}.tar
                echo "The docker saved to ${service}.tar"
            """

            def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()

            // Copy the Docker image to the GCP VM
            retry(3) {
                sh """                
                    scp -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ${service}.tar ubuntu@${IP}:/home/ubuntu/
                    echo "Copied to GCP VM"
                """
            }
            
            // SSH into the VM and load the Docker image
            retry(3) {
                sh """
                    ssh -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ubuntu@${IP} 'docker load -i /home/ubuntu/${service}.tar'
                    echo "Loaded into the GCP VM"
                """   
            }
            
            //Execute remote commands
            sh """
                ssh -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ubuntu@${IP} 'ddocker run -d ${eenv.AUTHENICATION_PORTS} ${sourceImage}'
            """                       
        }
    }      
}

return this