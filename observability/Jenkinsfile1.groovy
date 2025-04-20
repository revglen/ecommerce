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
    def CONSUL_IP = params.result 
    def FIREWALL_NAME="allow-web-traffic-obs"
    def RESOURCE_ID="projects/${env.GCP_PROJECT}/global/firewalls/${FIREWALL_NAME}"
    def TIMEOUT=300     // 5 minutes (300 seconds)
    def INTERVAL=10     // Check every 10 seconds
    def COMPLETED_STR="COMPLETED"
    def INSTANCE_NAME="obs-vm"
    def IP=""
    
    stage('Build observability Service') {   

        sh """
            echo "[INFO] Authenticating with gcloud using service account..."

            gcloud auth activate-service-account --key-file="$env.GOOGLE_CREDENTIALS"           
            gcloud config set project "$env.GCP_PROJECT"

            echo "[INFO] Deleting the firewall if the firewall rule '$FIREWALL_NAME' exists..."
            gcloud compute firewall-rules delete "$FIREWALL_NAME" --project="$env.GCP_PROJECT" --quiet || true
            echo "[INFO] Deleting the firewall if the firewall rule '$FIREWALL_NAME' exists..."

        """
        
        sh """
            gcloud compute instances create "${INSTANCE_NAME}" \\
                --project='${env.GCP_PROJECT}' \\
                --zone='${env.ZONE}' \\
                --machine-type=e2-standard-2 \\
                --image-family=ubuntu-2204-lts \\
                --image-project=ubuntu-os-cloud \\
                --tags=http-server,https-server \\
                --metadata-from-file startup-script=./startup_script.sh \\
                --metadata="ssh-keys=ubuntu:\$(cat ${env.SSH_PUB_KEY})"
                
        """    
            
        sh '''
            # Checking if the VM is up and startup_script has completed

            start_time=$(date +%s)
            end_time=$((start_time + ''' + TIMEOUT + '''))

            echo "⏳ Waiting for startup script on instance ''' + INSTANCE_NAME + ''' to complete..."

            for i in {1..''' + TIMEOUT/INTERVAL + '''}; do
                if status=\$(gcloud compute ssh ''' + INSTANCE_NAME + ''' \\
                    --zone=''' + env.ZONE + ''' \\
                    --command="cat /tmp/startup-script-complete 2>/dev/null" \\
                    --quiet 2>/dev/null) && \\
                [[ "\$status" == *''' + COMPLETED_STR + '''* ]]; then
                    echo "✅ \$status"
                    break
                fi
                printf "."
                sleep ''' + INTERVAL + '''
            done
        '''

        sh """
            gcloud compute firewall-rules create '$FIREWALL_NAME' \\
                --direction=INGRESS \\
                --priority=1000 \\
                --network=default \\
                --action=ALLOW \\
                --rules=tcp:80,tcp:443,tcp:9090,tcp:3000,tcp:22 \\
                --target-tags=http-server,https-server \\
                --description="Allow HTTP (80), HTTPS (443), and custom web traffic (8500)"
        """

        IP = sh(
            script: """
                gcloud compute instances list \\
                --project=${env.GCP_PROJECT} \\
                --filter="name=${INSTANCE_NAME}" \\
                --format='value(networkInterfaces[0].accessConfigs[0].natIP)' \\
                --quiet
            """.trim(),
            returnStdout: true
        ).trim()       

        echo "${env.WORKSPACE}/observability/${COMPOSE_FILE}"
        sh "echo \"CONSUL_HOST=${CONSUL_IP}\" > ${env.WORKSPACE}/observability/.env"
        sh "echo \"CONSUL_PORT=8500\" >> ${env.WORKSPACE}/observability/.env"
        sh "echo \"PROMETHEUS_ENABLED=true\" >> ${env.WORKSPACE}/observability/.env"
        sh "echo \"LOG_LEVEL=INFO\" >> ${env.WORKSPACE}/observability/.env"
        sh "cat ${env.WORKSPACE}/observability/.env"
        sh "docker compose -f ${env.WORKSPACE}/observability/${COMPOSE_FILE} up -d --build"
    }
    
    stage('Tag and Push auth Images') {
       
        def DOCKER_CONFIG = "${env.DOCKER_HOME}/.docker"
        def HOME = "${env.DOCKER_HOME}"    
        
        def services = sh(
            script: "docker compose -f ${env.WORKSPACE}/observability/${COMPOSE_FILE} config --services",
            returnStdout: true
        ).trim().split('\n')
                
        for (service in services) {
            echo "--- Processing service: ${service} ---"
        
            def sourceImage = sh(
                script: "docker compose -f ${env.WORKSPACE}/observability/${COMPOSE_FILE} config | grep 'image:' | grep '${service}'  | awk '{print \$2}'",
                returnStdout: true
            ).trim()
            
            echo "Source image from compose: '${sourceImage}'"
                                          
            // Save the Docker image as a tar file
            sh """
                docker save ${sourceImage} -o ${service}.tar
                echo "The docker saved to ${service}.tar"
            """

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

            if (service != "prometheus") {
                parameters = "-p 3000:3000"
            } else {
                parameters = "-p 9090:9090"
            }

            // Wait for port 22 to be open
            sh """
                for i in \$(seq 1 10); do
                    if nc -z "$IP" 22; then
                        echo "Port 22 is open for Docker command execution."
                        
                        if ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" "ubuntu@$IP" \
                        "docker load -i /home/ubuntu/${service}.tar && docker run --env-file ./.env -d $parameters $sourceImage"; then
                            echo "Docker commands executed successfully."
                            break
                        else
                            echo "Failed to execute Docker commands. Retrying attempt \$i/10"
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
    
    stage('Cleanup observability/ Containers') {
        echo "Stopping observability/ containers"
        sh 'docker stop $(docker ps -q) || true'
    }
}

return this