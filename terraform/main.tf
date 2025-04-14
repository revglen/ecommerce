provider "google" {
  project = var.project_id
  region  = var.region
}

resource "google_compute_instance" "docker_vm" {
  name         = "docker-vm"
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

  metadata_startup_script = <<-EOF
    #!/bin/bash
    # Install Docker
    sudo apt-get update
    sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    sudo apt-get update
    sudo apt-get install -y docker-ce
    sudo usermod -aG docker ${var.vm_username}
    
    # Install Docker Compose
    sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    
    # Enable Docker service
    sudo systemctl enable docker
    sudo systemctl start docker
  EOF

  tags = ["docker-host"]

  metadata = {
    ssh-keys = "${var.vm_username}:${file(var.ssh_public_key)}"
  }
}

output "instance_ip" {
  value = google_compute_instance.docker_vm.network_interface[0].access_config[0].nat_ip
}