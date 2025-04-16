
resource "google_compute_firewall" "allow_web_traffic_auth" {
  name        = "allow-web-traffic-auth"
  project     = var.project_id
  network     = "default"
  direction   = "INGRESS"
  priority    = 1000

  allow {
    protocol = "tcp"
    ports    = ["22", "80", "443", "8002"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]

  # Skip API error if rule already exists (non-Terraform managed)
  lifecycle {
    ignore_changes = [name, project]  # Prevents forced recreation
  }
}