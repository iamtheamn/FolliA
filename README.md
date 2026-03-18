# 🧠 FollIA - Private Neural Assistant

[![fr](https://img.shields.io/badge/Langue-Fran%C3%A7ais-blue)](https://github.com/iamtheamn/FolliA/blob/main/README.fr.md)

**FollIA** is a lightweight, clean, and highly customizable Android client designed to interact with your local Large Language Models (LLMs) via **[Ollama](https://ollama.com/)**. 

Because it takes a little madness to want to run an AI in your pocket, but a lot of seriousness to do it 100% privately.

## ✨ Key Features
- **100% Private (Edge AI)**: All processing stays on your local network or VPN. No cloud, no subscriptions, no data harvesting.
- **Smart Auto-Detection**: Enter your server's IP, and FollIA automatically scans and connects to the active LLM on your machine (Llama3, Phi, Mistral...). No manual model configuration needed!
- **Tailscale / VPN Friendly**: Access your home server (PC, Raspberry Pi) from anywhere using your 5G connection and a VPN like Tailscale.
- **Custom UI**: Make it yours with Light, Dark, or true AMOLED themes, and multiple accent colors.

## 📱 Minimum Requirements
**For the Android Device:**
- Android 8.0 (API 26) or higher.
- Active network connection (Local Wi-Fi or Mobile Data via VPN).

**For the Host Server:**
- A computer or SBC (like a Raspberry Pi 4/5) running [Ollama](https://ollama.com/).
- Minimum 4GB of RAM (8GB+ highly recommended to run models like *Llama3* or *Phi3* smoothly).

## 🚀 Getting Started

### Prerequisites
1. You need to have **Ollama** running on a computer or server.
2. Your Android device must be on the same network as the server.
3. *(Important)* Ensure Ollama is configured to listen to external IPs by setting the environment variable `OLLAMA_HOST=0.0.0.0` on your server.

### Installation
1. Go to the **[Releases](../../releases)** page.
2. Download the latest `app-release.apk`.
3. Install the APK on your Android device.
4. Open **FollIA**, go to Settings ⚙️, and enter your server's IP address.

# V1.0 comming soon with many changes !

## 📜 License
This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
