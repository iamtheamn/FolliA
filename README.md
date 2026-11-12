# 🧠 FolliA - Local and Native AI. Uncompromising Privacy.

[![en](https://img.shields.io/badge/Language-English-red)](https://github.com/iamtheamn/FolliA/blob/main/README.md)
[![fr](https://img.shields.io/badge/Langue-Fran%C3%A7ais-blue)](https://github.com/iamtheamn/FolliA/blob/main/README.fr.md)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/iamtheamn/FolliA?color=success)](#)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

🌍 **[Visit the Official FolliA Website](https://iamtheamn.github.io/FolliA-website/)**

**FolliA** is a lightweight, native, and 100% private Android client designed to interact with your local Large Language Models (LLMs) via **[Ollama](https://ollama.com/)**. 

Because it takes a little madness to want to run an AI in your pocket, but a lot of seriousness to do it privately and securely.

## 📸 Screenshots

<details>
  <summary>📱 <b>View Phone Interface (Click to expand)</b></summary>
  <br>
  <p align="center">
    <img src="images/chatscreenphone.png" width="250" alt="FolliA Chat Mobile" />
    <img src="images/settingsphone.png" width="250" alt="FolliA Settings Mobile" />
  </p>
</details>

<details>
  <summary>💻 <b>View Tablet Interface (Click to expand)</b></summary>
  <br>
  <p align="center">
    <img src="images/chatscreentab.png" width="600" alt="FolliA Chat Tablet" />
    <br><br>
    <img src="images/settingstab.png" width="600" alt="FolliA Settings Tablet" />
  </p>
</details>

## ✨ Core Features
- 🎙️ **Hands-Free Voice Conversations:** Talk with your AI by voice. Dictation runs entirely on the phone thanks to the open-source [Vosk](https://alphacephei.com/vosk/) engine: no Google service, no connection.
- 📌 **Organized Conversations:** Pin, rename or delete your chats, all stored on the device.
- ☁️ **Data Sovereignty & Nextcloud Sync:** Export your chats locally or sync them entirely privately via WebDAV (Nextcloud).
- 🛡️ **100% Private & Standalone:** All processing stays on your local network. Connect remotely using your own VPN setup. No third-party clouds, zero telemetry, no dependency on Google services. Server address and Nextcloud credentials are encrypted on the device.

## 📱 Minimum Requirements
**For the Android Device:**
- Android 7.0 (API 24) or higher.
- Active network connection (Local Wi-Fi or mobile data via VPN).

**For the Host Server:**
- A computer or SBC (like a Raspberry Pi) running [Ollama](https://ollama.com/).
- Minimum 4GB of RAM (8GB+ highly recommended for recent models).

## 🚀 Getting Started

### Prerequisites
1. **Ollama** must be running on your server.
2. Your Android device must be able to reach the server (same network or VPN).
3. *(Important)* Ensure Ollama is configured to listen to external IPs by setting the environment variable `OLLAMA_HOST=0.0.0.0` on your host machine.

### Installation
[![Get it on GitHub](https://img.shields.io/badge/Get_it_on-GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](../../releases/latest)
[![Get it on Obtainium](https://img.shields.io/badge/Get_it_on-Obtainium-7C4DFF?style=for-the-badge)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/iamtheamn/FolliA)

With **[Obtainium](https://github.com/ImranR98/Obtainium)**, FolliA updates automatically with every new release.

Manual installation:
1. Go to the project's **[Releases](../../releases)** page.
2. Download the `.apk` file of the latest version: `arm64-v8a` for most phones, `universal` if unsure.
3. Install the APK on your Android device.
4. Open **FolliA**, go to Settings ⚙️, and enter your server's IP address.
5. *(Optional)* For voice dictation, download your language model once in Settings > Voice.

### ⚠️ Updating from v1.0
Starting with v1.0.1, FolliA is signed with a new key, so Android will refuse to install v1.0.1 over v1.0:
1. In v1.0, export your conversations: Settings > Sync > Backup.
2. Uninstall v1.0, then install v1.0.1.
3. Restore your backup from Settings > Sync > Restore, then enter your server address again.

Privacy policy: [iamtheamn.github.io/FolliA-website/privacy.html](https://iamtheamn.github.io/FolliA-website/privacy.html)

## 🤝 Support the project
FolliA is developed entirely independently. If you find the app useful, consider leaving a ⭐ on this repository or buying me a coffee:
[![Ko-fi](https://img.shields.io/badge/Support_on-Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/iamtheamn404)

## 📜 License
This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
