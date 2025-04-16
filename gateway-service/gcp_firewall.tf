# main.tf

resource "google_compute_firewall" "allow_web_traffic_auth" {
  count = length(data.google_compute_firewall.existing.*.name) == 0 ? 1 : 0

  name        = var.firewall_name
  network     = "default"
  direction   = "INGRESS"
  priority    = 1000
  allow {
    protocol = "tcp"
    ports    = ["80", "443", "8500", "8300", "8301", "8600"]
  }
  allow {
    protocol = "udp"
    ports    = ["8600", "8301"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]

  # Ensure rule is deleted cleanly when removed from Terraform
  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_firewall" "allow_web_traffic_auth" {
  name        = "allow-web-traffic-auth"
  project     = var.project_id
  network     = "default"
  direction   = "INGRESS"
  priority    = 1000

  allow {
    protocol = "tcp"
    ports    = ["80", "443", "8500", "8300", "8301", "8600"]
  }
  allow {
    protocol = "udp"
    ports    = ["8600", "8301"]
  }
  
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]

  # Skip API error if rule already exists (non-Terraform managed)
  lifecycle {
    ignore_changes = [name, project]  # Prevents forced recreation
  }
}