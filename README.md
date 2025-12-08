# 📡 Blu-Drop: Decentralized, Internet-Free Messaging

## 🚀 Project Overview

**Blu-Drop** is a decentralized, peer-to-peer (P2P) messaging application developed as a B.Tech Final Year Project. It is designed to provide reliable communication in environments where traditional internet or cellular connectivity is unavailable or compromised.

Inspired by projects like BitChat, MeshConnect leverages **Bluetooth Low Energy (BLE) 5.0** to form a **self-healing mesh network**, allowing messages to hop securely from one device to the next until they reach their destination.

### The Problem

Modern communication is entirely dependent on centralized internet infrastructure. In disaster zones, remote areas, or situations of network failure, communication often halts completely.

### The Solution

MeshConnect creates an **ad-hoc, localized network** where every device acts as a relay, dramatically extending the communication range far beyond the standard Bluetooth limit.

---

## ✨ Core Features

* **Peer-to-Peer Communication:** Direct, server-less messaging between users within the mesh network.
* **Managed Flooding Protocol:** Custom application-layer protocol ensuring messages efficiently propagate across multiple hops (devices) without relying on a central router.
* **End-to-End Encryption (E2EE):** All messages are secured using industry-standard **AES-GCM** cryptography, ensuring privacy even when messages are relayed by untrusted nodes.
* **Store-and-Forward:** Messages destined for offline users are temporarily cached by relay nodes and delivered automatically upon the recipient's reconnection.
* **Decentralized Addressing:** Unique device identifiers and addressing scheme for robust routing.
* **Intuitive Android UI:** Clean interface for chat, connection status, and mesh diagnostics.

---

## 💻 Technical Architecture & Stack

The core innovation of Blue-Drop lies in the **Custom Mesh Protocol** built on top of the low-level Android BLE API.

### Tech Stack

| Component | Technology | Role |
| :--- | :--- | :--- |
| **Platform** | **Android** | Primary application platform. |
| **Language** | **Java** |  
| **Networking** | **Bluetooth Low Energy (BLE) 5.0+** | Low-power hardware layer for P2P communication (Advertising & Scanning). |
| **Protocol** | **Custom Layer 7 Protocol** | Defines message format, TTL, addressing, and relay logic. |
| **Security** | **Bouncy Castle / JCA** | Implementation of AES-GCM for E2EE and key management. |
| **Data Persistence** | **Room Persistence Library (SQLite)** | Local storage for chat history and cached messages (Store-and-Forward). |

### 🧠 How the Mesh Works

1.  **Message Initiation:** A message is encrypted and encapsulated with a unique ID, a TTL (Time-to-Live) counter, and the recipient's address.
2.  **Broadcasting:** The sending device broadcasts the message payload as a BLE Advertisement Packet.
3.  **Relay:** Nearby devices (Mesh Nodes) receive the advertisement.
    * If the message is for them, they decrypt and display it.
    * If the message is for another node and the TTL is greater than zero, the receiving node decrements the TTL and re-broadcasts the message, extending its reach.
4.  **Reliability:** The TTL mechanism prevents infinite looping, and message IDs prevent redundant re-broadcasting, ensuring efficient **managed flooding**.

---

## ⚙️ Setup and Installation

### Prerequisites

* Android Studio (Latest Version)
* Physical Android devices (at least two) running Android 6.0+ with BLE 5.0 support for optimal performance.

### Steps to Run

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/Adityabisht794/Blue-Drop]
    ```
2.  **Open in Android Studio:**
    * Open the project folder in Android Studio.
3.  **Build and Deploy:**
    * Connect multiple Android devices via USB.
    * Select `Run` -> `Run 'app'` and choose your devices to deploy the APK simultaneously.
4.  **Enable Permissions:**
    * Ensure Bluetooth and Location services are enabled on all test devices (required for BLE scanning).

---

## 👥 Team & Roles

This project was developed by a team of four B.Tech Final Year students.

| Name | Role | Focus Areas |
| :--- | :--- | :--- |
| [Gaurav Mehra & Lucky Tiwari] | **Network Architect / BLE Lead** | Custom Protocol, BLE Advertising/Scanning Logic, Node Provisioning. |
| [Aditya Bisht] | **Security & Protocol Specialist** | End-to-End Encryption (AES-GCM), Key Exchange, Message Reliability/ACKs. |
| [Lucky Tiwari & Gaurav Mehra] | **Application & UI Developer** | Android UI/UX, Activity/Fragment Management, Front-end Integration. |
| [Dev Joshi] | **Data & Persistence Engineer** | Room Database Schema, Store-and-Forward Logic, Data Flow (Flow/LiveData). |

---

## 📄 License

This project is open-sourced under the [Choose a License, e.g., MIT] License. See the `LICENSE` file for more details.
