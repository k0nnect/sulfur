# 🧪 Sulfur

Sulfur is a new, fast, lightweight, open-source **.JAR file decompiler and bytecode viewer** built in Java with a Swing UI. 

Think of it as a simpler [Recaf](https://github.com/Col-E/Recaf)/[Jbytedit](https://github.com/Eyremba/JBytedit) — drop in any .JAR file and instantly browse its classes, view **decompiled Java** (Procyon or CFR) and **readable bytecode** (ASM Textifier).

---

## ✨ Features (WIP)
- 🔍 **JAR Browser** — opens any `.jar` file and shows you the entire package/class tree
- 🪄 **Dual View** — displays decompiled Java and ASM bytecode side-by-side
- ⚡ **Search function** — live filter classes by name
- 🛠️ **One-click run** — no config needed; just open JARs on the fly!
- 🧩 **Extensible** — plug in additional decompilers or UI modules

---

## Requirements
- [Java 17+](https://www.oracle.com/java/technologies/downloads/) (tested with Java 21)
- [Gradle 8+](https://github.com/gradle/gradle) (wrapper included)

## 🚀 Installation

### Build & Run
```bash
git clone https://github.com/k0nnect/sulfur.git
cd sulfur
./gradlew run

```
Or alternatively, you can build a runnable JAR:
```bash
./gradlew fatJar
java -jar build/libs/sulfur-all.jar
```

### 🤝 Contributing

Pull requests are welcome! If you’re adding a feature or fixing a bug, open an issue first to discuss what you’d like to change.

### 📜 License
* [MIT License](https://en.wikipedia.org/wiki/MIT_License)

### Credits
* [ASM 9.8](https://asm.ow2.io/) — for bytecode parsing & text output
* [Procyon](https://github.com/mstrobel/procyon)/[CFR](https://www.benf.org/other/cfr/) — decompiler engine(s)
* [Java Swing](https://en.wikipedia.org/wiki/Swing_%28Java%29) — UI <3










