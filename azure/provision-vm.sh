#!/usr/bin/env bash
# Creates the Azure resources for CampusFix with the Azure CLI (az login first).
# Usage: bash azure/provision-vm.sh            (optional: LOCATION=centralindia RG=campusfix-rg VM=campusfix-vm)
set -euo pipefail

RG="${RG:-campusfix-rg}"
VM="${VM:-campusfix-vm}"
LOCATION="${LOCATION:-centralindia}"
SIZE="${SIZE:-Standard_B2s}"
ADMIN_USER="${ADMIN_USER:-azureuser}"

az group create --name "$RG" --location "$LOCATION"

az vm create \
  --resource-group "$RG" --name "$VM" \
  --image Ubuntu2204 --size "$SIZE" \
  --admin-username "$ADMIN_USER" \
  --generate-ssh-keys \
  --public-ip-sku Standard

# Application port (SSH 22 is opened by default)
az vm open-port --resource-group "$RG" --name "$VM" --port 8080 --priority 1010

# Jenkins (only if you install Jenkins on this same VM):  az vm open-port ... --port 8081 --priority 1020

IP=$(az vm show -d -g "$RG" -n "$VM" --query publicIps -o tsv)
echo "VM public IP: $IP"
echo "SSH:          ssh $ADMIN_USER@$IP"
echo "Put the IP in ansible/inventory.ini, then run the Ansible playbook."
echo "App URL after deployment: http://$IP:8080"
echo "To avoid charges later: az group delete --name $RG --yes --no-wait"
