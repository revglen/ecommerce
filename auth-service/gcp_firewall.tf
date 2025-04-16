resource "google_compute_firewall" "allow_web_traffic" {
  name        = "allow-web-traffic"
  network     = "default"
  direction   = "INGRESS"
  priority    = 1000
  allow {
    protocol = "tcp"
    ports    = ["80", "443", "8002", "22"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]
}

import {
  to = google_compute_firewall.allow_web_traffic
  id = "projects/testo-455513/global/firewalls/allow-web-traffic"
}