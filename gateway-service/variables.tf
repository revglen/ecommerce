variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region"
  type        = string
  default     = "europe-west2"
}

variable "zone" {
  description = "The GCP zone"
  type        = string
  default     = "europe-west2-b"
}

variable "machine_type" {
  description = "The machine type for the VM"
  type        = string
  default     = "e2-medium"
}

variable "vm_username" {
  description = "Username for the VM"
  type        = string
  default     = "ubuntu"
}

variable "ssh_public_key" {
  description = "SSH public key content"
  type        = string
  sensitive   = true  # Marks the variable as sensitive
}

variable "firewall_name" {
  description = "Name of the firewall rule"
  type        = string
  default     = "allow-web-traffic-auth"
}
