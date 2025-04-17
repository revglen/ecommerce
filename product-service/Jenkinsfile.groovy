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
    env.SECRET_KEY=params.secret_key
    env.API_KEY=params.api_key
    env.ALGORITHM=params.algorithm
    env.ACCESS_TOKEN_EXPIRE_MINUTES=params.token_expiry
    env.CONSUL_ADDRESS = params.result 
    
    def COMPOSE_FILE = 'docker-compose.yml'
    def FIREWALL_NAME="allow-web-traffic-product"
    def RESOURCE_ID="projects/${env.GCP_PROJECT}/global/firewalls/${FIREWALL_NAME}"
    def CONSUL_ADDRESS = params.result
    
    stage('Call Terraform and create a VM in GCP') {       

        sh 'terraform init'
        //sh 'terraform plan -out=tfplan'
        //sh 'terraform show tfplan'

        sh """
            echo "[INFO] product with gcloud using service account..."

            gcloud auth activate-service-account --key-file="$env.GOOGLE_CREDENTIALS"           
            gcloud config set project "$env.GCP_PROJECT"

            echo "[INFO] Deleting the firewall if the firewall rule '$FIREWALL_NAME' exists..."
            gcloud compute firewall-rules delete "$FIREWALL_NAME" --project="$env.GCP_PROJECT" --quiet || true
            echo "[INFO] Deleting the firewall if the firewall rule '$FIREWALL_NAME' exists..."
        """  

        sh """
            terraform apply -auto-approve \
                -var="ssh_public_key=${env.SSH_PUB_KEY}" \
                -var="project_id=${env.TF_VAR_project_id}"
        """

        def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()
        sh "ssh-keygen -R ${IP} || true"
        sh "echo The Consul is ${env.CONSUL_ADDRESS}"        

        sh "echo \"CONSUL_HOST=${env.CONSUL_ADDRESS}\" > ${env.WORKSPACE}/product-service/.env"
        sh "echo \"EXTERNAL_HOST_IP=${IP}\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"CONSUL_PORT=8500\" >> ${env.WORKSPACE}/product-service/.env"

        sh "echo \"DB_USER=product_user\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"DB_PASSWORD=product_password\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"DB_HOST=localhost\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"DB_PORT=5432\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"DB_NAME=product_db\" >> ${env.WORKSPACE}/product-service/.env"

        sh "echo \"SECRET_KEY=${env.SECRET_KEY}\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"API_KEY=${env.API_KEY}\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"ALGORITHM=${env.ALGORITHM}\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"ACCESS_TOKEN_EXPIRE_MINUTES=${env.ACCESS_TOKEN_EXPIRE_MINUTES}\" >> ${env.WORKSPACE}/product-service/.env"

        sh "echo \"CORS_ORIGINS=http://localhost:3000,http://127.0.0.1:3000\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"PROMETHEUS_ENABLED=true\" >> ${env.WORKSPACE}/product-service/.env"
        sh "echo \"LOG_LEVEL=INFO\" >> ${env.WORKSPACE}/product-service/.env"

        sh "cat ${env.WORKSPACE}/product-service/.env"
        sh "docker compose -f ${env.WORKSPACE}/product-service/${COMPOSE_FILE} up -d --build"
    }
    
    stage('Tag and Push product Images') {
       
        def DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
        def HOME = "${env.DOCKER_HOME}"    
        
        def services = sh(
            script: "docker compose -f ${env.WORKSPACE}/product-service/${COMPOSE_FILE} config --services",
            returnStdout: true
        ).trim().split('\n')
                
        for (service in services) {
            echo "--- Processing service: ${service} ---"
        
            def sourceImage = sh(
                script: "docker compose -f ${env.WORKSPACE}/product-service/${COMPOSE_FILE} config | grep 'image:' | grep -A5 '${service}' | awk '{print \$2}'",
                returnStdout: true
            ).trim()
            
            echo "Source image from compose: '${sourceImage}'"
                                          
            // Save the Docker image as a tar file
            sh """
                docker save ${sourceImage} -o ${service}.tar
                echo "The docker saved to ${service}.tar"
            """

            def IP = sh(script: 'terraform output -raw instance_ip', returnStdout: true).trim()

            // Wait for port 22 to be open
            sh """
                for i in \$(seq 1 10); do
                    if nc -z -w 5 ${IP} 22; then
                        echo "Port 22 is open, attempting SCP..."
                        if scp -o StrictHostKeyChecking=no \
                            -o ConnectTimeout=10 \
                            -i ${env.SSH_KEY} \
                            ${service}.tar .env ubuntu@${IP}:/home/ubuntu/; then
                            echo "Successfully copied to GCP VM"
                            break
                        else
                            echo "SCP attempt \$i failed"
                        fi
                    else
                        echo "SSH not available yet (attempt \$i/10)"
                    fi
                    sleep 5
                done
            """

            def parameters = ""
            if (${service} != "postgres"){
                parameters = "-p 8001:8001"
            }
            else {
                parameters = " -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432"
            }

            // Wait for port 22 to be open
            sh """
                for i in \$(seq 1 10); do
                    if nc -z "$IP" 22; then
                        echo "Port 22 is open for Docker command execution."
                        
                        if ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" "ubuntu@$IP" \
                        "docker load -i /home/ubuntu/${service}.tar && docker run --env-file ./.env -d ${parameters} $sourceImage"; then
                            echo "Docker commands executed successfully."
                            break
                        else
                            echo "Failed to execute Docker commands. Retrying..."
                            sleep 5
                        fi
                    else
                        echo "Waiting for SSH to be available..."
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