#!/bin/bash

# BLE Browser-Android Bridge - GitHub Setup Script
# This script helps you create and configure the GitHub repository

echo "🚀 BLE Browser-Android Bridge - GitHub Setup"
echo "============================================="
echo

# Check if GitHub CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed."
    echo "Please install it first: https://cli.github.com/"
    echo "Or create the repository manually on GitHub."
    exit 1
fi

# Check if user is logged in to GitHub CLI
if ! gh auth status &> /dev/null; then
    echo "🔐 Please login to GitHub CLI first:"
    echo "gh auth login"
    exit 1
fi

echo "📝 Repository Details:"
echo "Name: ble-browser-android-bridge"
echo "Description: Bluetooth Low Energy communication bridge between web browsers and Android devices"
echo "Visibility: Private"
echo

read -p "Do you want to create this repository? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Repository creation cancelled."
    exit 1
fi

echo "🔨 Creating GitHub repository..."

# Create the repository
gh repo create ble-browser-android-bridge \
    --private \
    --description "Bluetooth Low Energy communication bridge between web browsers and Android devices" \
    --add-readme=false

if [ $? -ne 0 ]; then
    echo "❌ Failed to create repository. It might already exist."
    echo "You can create it manually at: https://github.com/new"
    exit 1
fi

echo "✅ Repository created successfully!"
echo

echo "📤 Adding remote and pushing code..."

# Add remote origin
git remote add origin "https://github.com/$(gh api user --jq .login)/ble-browser-android-bridge.git"

# Push to GitHub
git push -u origin main

if [ $? -eq 0 ]; then
    echo "✅ Code pushed successfully!"
    echo
    echo "🎉 Setup Complete!"
    echo "=================="
    echo
    echo "📱 Your repository: https://github.com/$(gh api user --jq .login)/ble-browser-android-bridge"
    echo "🌐 Web client will be available at: https://$(gh api user --jq .login).github.io/ble-browser-android-bridge"
    echo
    echo "📋 Next Steps:"
    echo "1. Enable GitHub Pages in repository settings"
    echo "2. Create a release tag to trigger APK build: git tag v1.0.0 && git push origin v1.0.0"
    echo "3. Download APK from releases page and install on Android device"
    echo "4. Test the web client with your Android device"
    echo
    echo "🔧 To enable GitHub Pages:"
    echo "1. Go to repository Settings > Pages"
    echo "2. Select 'GitHub Actions' as source"
    echo "3. The web client will be deployed automatically"
else
    echo "❌ Failed to push code. Please check your permissions and try again."
    exit 1
fi
