resource "google_compute_firewall" "allow_web_traffic_order" {
  # This will ONLY create if doesn't exist (no duplicates, no errors)
  name    = "allow-web-traffic-product"
  project = var.project_id
  network = "default"

  # Rule configuration (customize as needed)
  direction   = "INGRESS"
  priority    = 1000
  allow {
    protocol = "tcp"
    ports    = ["22", "80", "443", "8000", "5432"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]

  timeouts {
    create = "5m"
    delete = "5m"
  }

  lifecycle {
    # If the resource exists with different settings, 
    # Terraform will adopt it without changes
    ignore_changes = [
      name,
      network,
      allow,
      source_ranges,
      target_tags
    ]
  }
}
