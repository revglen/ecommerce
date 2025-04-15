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

        sleep(3)
        
        def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()
        sh "ssh-keygen -R ${IP} || true"
        sh "echo \"CONSUL_HOST=${env.CONSUL_IP}\" > ${env.WORKSPACE}/order-service/.env"
        sh "echo \"CONSUL_PORT=8500\" >> ${env.WORKSPACE}/order-service/.env"
        sh "echo \"EXTERNAL_HOST_IP=${IP}\" >> ${env.WORKSPACE}/order-service/.env"

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
    
    stage('Tag and Push order Images') {
       
        def DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
        def HOME = "${env.DOCKER_HOME}"    
        
        def services = sh(
            script: "docker compose -f ${env.WORKSPACE}/order-service/${COMPOSE_FILE} config --services",
            returnStdout: true
        ).trim().split('\n')
                
        for (service in services) {
            echo "--- Processing service: ${service} ---"
        
            def sourceImage = sh(
                script: "docker compose -f ${env.WORKSPACE}/order-service/${COMPOSE_FILE} config | grep -A15 '${service}:' | grep 'image:' | awk '{print \$2}'",
                returnStdout: true
            ).trim()
            
            echo "Source image from compose: '${sourceImage}'"
                                          
            // Save the Docker image as a tar file
            sh """
                docker save ${sourceImage} -o ${service}.tar
                echo "The docker saved to ${service}.tar"
            """

            def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()

            def port = ""
            if (service == 'api-gateway') {
                port = "-p 5432:5432"
            }
            else {
                port = "-p 8001:8001"
            }

            sh """
                for i in {1..10}; do
                    if nc -z ${IP} 22; then
                        echo "Port 22 is open"
                        scp -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ${service}.tar ubuntu@${IP}:/home/ubuntu/
                        echo "Copied to GCP VM"
                        break
                    else
                        echo "Waiting for SSH..."
                        sleep 5
                    fi                   
                done
            """

            // Wait for port 22 to be open
            sh """
                for i in {1..10}; do
                    if nc -z ${IP} 22; then
                        echo "Port 22 is open"
                         ssh -o StrictHostKeyChecking=no -i ${env.SSH_KEY} ubuntu@${IP} 'docker load -i /home/ubuntu/${service}.tar && docker run -d -p 8002 ${sourceImage}'
                        echo "Copied to GCP VM"
                        break
                    else
                        echo "Waiting for SSH..."
                        sleep 5
                    fi                   
                done
            """
            
            sh """            
                rm -rf ${service}.tar
                echo "Deleted the tar file ${service}.tar"
            """                     
        }
    }      
}

return this