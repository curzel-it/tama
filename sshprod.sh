#!/bin/bash

# Load environment variables from .env
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
else
    echo "Error: .env file not found"
    exit 1
fi

# Check if required variables are set
if [ -z "$SERVER_ADDRESS" ] || [ -z "$SERVER_ROOT_PASSWORD" ]; then
    echo "Error: SERVER_ADDRESS or SERVER_ROOT_PASSWORD not set in .env"
    exit 1
fi

# Connect to server using sshpass if available, otherwise provide instructions
if command -v sshpass &> /dev/null; then
    sshpass -p "$SERVER_ROOT_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SERVER_ADDRESS
else
    echo "sshpass not found. Install it with: brew install sshpass (macOS) or apt-get install sshpass (Linux)"
    echo "Alternatively, connecting without sshpass (you'll need to enter the password manually):"
    echo "Password: $SERVER_ROOT_PASSWORD"
    ssh root@$SERVER_ADDRESS
fi
