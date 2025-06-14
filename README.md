# ☄️ Comet Messenger

A cosmic journey through code and conversation.
A minimal, modern JavaFX chat messenger built from scratch with a server/client architecture and a sprinkle of celestial charm ✨

---

## 💡 Features

- 🌐 **Multi-Client TCP Server**: Robust server supporting multiple simultaneous clients.
- 🖥️ **JavaFX GUI**: Clean, modern interface for seamless chat experiences.
- 🪪 **User Authentication**: Secure signup/login with embedded Postgres database.
- 🗂️ **Contacts & Chats**: Add contacts, create chats, and manage your conversations.
- 💬 **Real-Time Messaging**: Instant message delivery with WebSocket support.
- 🖼️ **Profile Management**: Update your display name and avatar.
- 📜 **Comprehensive Logging**: Track application events and debug issues easily.
- 🧪 **Unit Tests**: Core logic covered by JUnit tests.

---

## 🚀 Tech Stack


<div align="center">

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/javafx-%23FF0000.svg?style=for-the-badge&logo=javafx&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/apachemaven-C71A36.svg?style=for-the-badge&logo=apachemaven&logoColor=white)

</div>

- **Java 21**
- **JavaFX** (UI)
- **Maven** (build tool)
- **HikariCP** (connection pooling)
- **Postgres** (database)
- **JUnit** (testing)

---

## 🛠️ Build & Run

### Prerequisites

- Java 21 or newer
- Maven

---

## 🗺️ Project Structure

```text
comet-messenger/
├── src/
│   ├── main/
│   │   ├── java/com/comet/
│   │   │   ├── controller/
│   │   │   ├── db/
│   │   │   │   ├── repository/
│   │   │   │   └── schema/
│   │   │   ├── demo/
│   │   │   │   ├── core/
│   │   │   │   │   ├── client/
│   │   │   │   │   └── server/
│   │   │   │   └── App.java
│   │   │   └── module-info.java
│   │   └── resources/com/comet/...
│   └── test/java/com/comet/...
├── pom.xml
└── README.md
```

---

## 🧭 Roadmap

- [x] Multi-client TCP server
- [x] JavaFX GUI for chat
- [x] User authentication (signup/login)
- [x] Contact management
- [x] Profile editing
- [x] WebSocket integration
- [x] Group chat support
- [ ] Message status (sent, delievered, read)
- [ ] File sharing
- [ ] Emoji reactions
- [ ] Mobile client (future)

And massive refactoring as well. Such bloated codebase is impossible to scale, so I'll definitely have to update it and split into mane smaller files for proper management and future scalability purposes.

---

## (T^T) Known Issues

- [ ] When firstly selecting a group chat, it appears empty. Messages appear after second selection of that chat.
- [ ] I suck at writing tests for such kind of anapp :_)
- [ ] Message notifications work only in private chats. User PFP is not loaded to the notification tray

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

---

## 📜 License

Open Source vibes — MIT License

---

✉️ Message across the stars. Let the code be poetry, and the chats be legends.