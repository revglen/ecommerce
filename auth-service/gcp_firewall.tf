
# main.tf
data "google_compute_firewall" "existing" {
  name    = var.firewall_name
  project = var.project_id
}

resource "google_compute_firewall" "allow_web_traffic_auth" {
  count = length(data.google_compute_firewall.existing.*.name) == 0 ? 1 : 0

  name        = var.firewall_name
  network     = "default"
  direction   = "INGRESS"
  priority    = 1000
  allow {
    protocol = "tcp"
    ports    = ["22", "80", "443", "8002"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]

  # Ensure rule is deleted cleanly when removed from Terraform
  lifecycle {
    create_before_destroy = true
  }
}

