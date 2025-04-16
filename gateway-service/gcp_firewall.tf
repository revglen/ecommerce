resource "google_compute_firewall" "allow_web_traffic" {
  name        = "allow-web-traffic"
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
}