# Blu-Drop 📡
### Decentralized Peer-to-Peer Offline Messaging System using Google Nearby Connections API

---

# 🚀 Overview

**Blu-Drop** is a decentralized peer-to-peer (P2P) offline messaging application developed as a **B.Tech Final Year Project**. The system enables communication between Android devices without requiring internet connectivity, mobile data, centralized servers, or cellular infrastructure.

The application leverages the **Google Nearby Connections API** with the **P2P_CLUSTER** networking strategy to establish localized communication using Bluetooth and Wi-Fi Direct technologies. Devices dynamically discover nearby peers, establish direct connections, and relay messages across multiple nodes using a managed flooding mechanism.

Blu-Drop is designed for emergency communication, disaster recovery, remote areas, and offline environments where conventional communication systems may fail or become unavailable.

---

# 🎯 Problem Statement

Modern messaging applications heavily depend on:

- Internet connectivity
- Cellular networks
- Centralized cloud infrastructure
- Stable network availability

In situations such as:

- Natural disasters
- Remote regions
- Network outages
- Emergency response operations
- Internet shutdowns

traditional communication systems often fail completely.

Blu-Drop addresses this challenge by creating a decentralized communication network capable of functioning independently of internet infrastructure.

---

# 💡 Proposed Solution

Blu-Drop implements a peer-to-peer communication system where Android devices form a localized decentralized network using Bluetooth and Wi-Fi Direct technologies.

The system supports:

- Direct device-to-device communication
- Multi-hop message propagation
- Dynamic peer discovery
- Relay-based message forwarding
- Offline communication without servers

Messages propagate across connected devices using a managed flooding mechanism with duplicate suppression and hop tracking.

---

# ✨ Core Features

## 🔗 Multi-Hop Communication
Messages are relayed through multiple devices, extending communication beyond direct wireless range.

## 📡 Managed Flooding Protocol
Blu-Drop uses controlled rebroadcasting combined with duplicate suppression to minimize redundant transmissions.

## 🧠 Adaptive Peer-to-Peer Topology
The network dynamically adapts when devices join or leave the communication cluster.

## 🆔 UUID-Based Duplicate Detection
Each message contains a unique identifier to prevent duplicate processing and rebroadcast loops.

## 📶 P2P Cluster Networking
Uses Google Nearby Connections API with the `P2P_CLUSTER` strategy for scalable multi-device communication.

## 📊 Hop Count & Relay Path Tracking
Tracks propagation paths and hop counts for network diagnostics and visualization.

## 🔐 Secure Transport Communication
Communication channels are protected using the encrypted transport mechanisms provided by the Google Nearby Connections API.

## 📱 Modern Android Architecture
Built using MVVM and Clean Architecture principles for modularity, scalability, and maintainability.

---

# 🧠 System Architecture

## Communication Workflow

1. Device starts advertising and discovery
2. Nearby peers are detected automatically
3. Connections are established dynamically
4. Messages are serialized into payload objects
5. Payloads are broadcast to connected peers
6. Receiving nodes:
   - Check duplicate cache
   - Increment hop count
   - Update relay path
   - Rebroadcast unseen messages

---

# 🏗️ Technical Stack

| Component | Technology | Purpose |
|---|---|---|
| Platform | Android | Mobile application platform |
| Language | Kotlin / Java | Application development |
| Connectivity API | Google Nearby Connections API | Peer-to-peer communication |
| Networking Strategy | P2P_CLUSTER | Multi-device networking |
| Communication Medium | Bluetooth & Wi-Fi Direct | Device discovery and transfer |
| Architecture | MVVM + Clean Architecture | Modular system structure |
| UI Framework | Jetpack Compose | Reactive UI development |
| State Management | MutableStateFlow | Real-time UI updates |
| Build Tool | Gradle | Build automation |
| IDE | Android Studio | Development environment |

---

# 📡 Managed Flooding Mechanism

Blu-Drop uses a relay-based message dissemination model.

## Message Flow

```text
Device A → Nearby Devices → Relay Nodes → Destination Devices
