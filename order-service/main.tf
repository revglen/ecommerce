provider "google" {
  project = var.project_id
  region  = var.region
}

# Generate secure SSH key pair
resource "tls_private_key" "vm_ssh" {
  algorithm = "ED25519"  # More secure than RSA
  rsa_bits  = 4096      # Only used if algorithm were RSA
}

# Save private key securely (not in state file)
resource "local_sensitive_file" "private_key" {
  filename        = "${path.module}/.ssh/gcp_vm_key"
  content         = tls_private_key.vm_ssh.private_key_openssh
  file_permission = "0600"  # Owner read/write only
}

# Save public key (optional)
resource "local_file" "public_key" {
  filename        = "${path.module}/.ssh/gcp_vm_key.pub"
  content         = tls_private_key.vm_ssh.public_key_openssh
  file_permission = "0644"
}

resource "google_compute_instance" "docker_vm" {
  name         = "order-vm"
  machine_type = var.machine_type
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
    }
  }

  network_interface {
    network = "default"
    access_config {
      // Ephemeral public IP
    }
  }

  metadata = {
    ssh-keys = "ubuntu:${file("/var/lib/jenkins/.ssh/jenkins_gcp_ssh.pub")}"
  } 

  metadata_startup_script = <<-EOF
    #!/bin/bash
    # Docker installation script
    sudo apt-get update
    sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
    sudo usermod -aG docker ${var.vm_username}
    sudo systemctl status ssh
    sudo apt install ufw -y
    sudo ufw enable
    sudo ufw allow 22/tcp
    sudo ufw allow 8001/tcp
    sudo ufw allow 80/tcp
    sudo ufw allow 443/tcp
    sudo ufw allow 5432/tcp
  EOF

  tags = ["docker-host","http-server","https-server"]
}

output "instance_ip" {
  value = google_compute_instance.docker_vm.network_interface[0].access_config[0].nat_ip
}

output "ssh_private_key" {
  value     = tls_private_key.vm_ssh.private_key_openssh
  sensitive = true
}

output "ssh_command" {
  value = "ssh -i .ssh/gcp_vm_key ${var.vm_username}@${google_compute_instance.docker_vm.network_interface[0].access_config[0].nat_ip}"
}