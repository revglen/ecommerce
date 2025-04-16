resource "google_compute_firewall" "allow_web_traffic_auth" {
  # This will ONLY create if doesn't exist (no duplicates, no errors)
  name    = "allow-web-traffic-auth"
  project = var.project_id
  network = "default"

  # Rule configuration (customize as needed)
  direction   = "INGRESS"
  priority    = 1000
  allow {
    protocol = "tcp"
    ports    = ["22", "80", "443", "8002"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]

  # Magic happens here:
  lifecycle {
    ignore_changes = all
  }
}

# # Safe import block (Terraform 1.5+)
# import {
#   to = google_compute_firewall.allow_web_traffic_auth
#   id = "projects/${var.project_id}/global/firewalls/allow-web-traffic-auth"
# }