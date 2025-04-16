output "instance_name" {
  value = google_compute_instance.docker_vm.name
}

# output "instance_ip" {
#   value = google_compute_instance.docker_vm.network_interface[0].access_config[0].nat_ip
# }

output "firewall_status" {
  value = length(data.google_compute_firewall.existing.*.name) == 0 ? "Created new rule" : "Rule already existed (no changes)"
}