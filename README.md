# Vessel

# Setting Up GitHub & Cloning the Repository in IntelliJ IDEA

## 1. Prerequisites

Before starting, ensure you have:

* [Git](https://git-scm.com/downloads) installed on your computer.
* [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Community or Ultimate Edition)
* The **GitHub account** where you received the repository invite in.
* Java's JDK 21
* [Maven](https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip) installed on your computer.
  
---
## 2. Configuring Maven
* Install it using [Maven](https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip).

### 2.1 Extract Maven.zip
![img.png](readme.meta/image.png)
![img.png](readme.meta/image2.png)

### 2.2 Copy path
It should look like this:
``` C:\Users\prath\Downloads\apache-maven-3.9.11-bin (2)\apache-maven-3.9.11\bin ```

### 2.3 Adding Maven to path
![img.gif](readme.meta/gigi.gif)

### 2.4 Sanity check.
Open command prompt and use ``` mvn --version ```
![img.png](readme.meta/mvn.png)

----

## 3. Cloning the Private Repository

1. In IntelliJ, go to **File → New → Project from Version Control → Git**.
2. Paste the repository URL:

   ```
   https://github.com/prxcode/vessel.git
   ```
3. Choose a destination folder and click **Clone**.
4. If prompted, log in with your GitHub credentials or personal access token.

---

## 4. Opening the Project

* Once cloned, IntelliJ will automatically open the project.
* If prompted, click **“Trust Project”**.
* Wait for IntelliJ to index and import dependencies (Gradle/Maven/etc.).

---

## 5. Basic Git Operations in IntelliJ

![img.png](readme.meta/git_status.png)

### Check Git Status

* Open the **Git** tool window at the bottom (or use `Alt + 9`/ `Ctrl + Shift + G` / `Cmd + 9`).
* You’ll see all modified files, main commits and branched commits listed.

---

![img.png](readme.meta/git_branch_menu.png)

### Commit Changes

1. In the top branch icon click **Commit** or press `Ctrl + K` / `Cmd + K`.
2. Write a meaningful commit message (eg: "Added cell creation logic").
3. Select the files to commit.
4. Click **Commit** or **Commit and Push**.

* Its healthier to make multiple commits per feature/update being implemented, rather than having one messy commit having too many features in it, making it harder to track

### Push Changes

* Use **Push** or shortcut `Ctrl + Shift + K` / `Cmd + Shift + K` to push changes to the remote server.

### Pull Updates

* The blue arrow next to `main` (or whatever branch you're on) indicates new changes from remote ready to be pulled
* Use **Pull** to sync the latest changes from the remote branch.

### Branch Management

* Create, switch, or merge branches directly from the menu.
> 💡 Push major changes to branches, rather than main to avoid disrupting stable code; merge back once completed
---
### 🧠 How to Add a Dependency

1. Open your project’s `pom.xml`.
2. Inside the `<dependencies>` tag, add the library you need in this format:

```xml
<dependencies>
    <!-- Example: Adding Gson for JSON parsing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.11.0</version>
    </dependency>

    <!-- Example: JavaFX Controls -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>

    <!-- Add more dependencies below -->
</dependencies>
```

---

### 🔗 Adding Local JARs (If Not Available on Maven Central)

If your library is not available online (for example, `ikonli-core.jar` in `/lib`), you can manually include it in `pom.xml` like this:

```xml
<dependency>
    <groupId>local.libs</groupId>
    <artifactId>ikonli-core</artifactId>
    <version>12.3.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/ikonli-core-12.3.1.jar</systemPath>
</dependency>
```

---
### Project Structre
```
vessel/
│
├── lib/
│   ├── ikonli-core-12.3.1.jar
│   ├── ikonli-fontawesome5-pack-12.3.1.jar
│   └── ikonli-javafx-12.3.1.jar
│
├── misc_images/
├── notebooks/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── vessel/
│   │   │           ├── model/
│   │   │           ├── kernel/
│   │   │           ├── frontendhelpers/
│   │   │           └── ...
│   │   └── resources/   ← ✅ Add any FXML, config, or assets here
│   │
│
├── target/
├── pom.xml   ← Required for Maven build (Right click on it and select '+add as maven project.')
├── README.md
└── .gitignore
```
---
---

### ⚙️ Basic Maven Commands

| Command | Description |
|----------|-------------|
| `mvn -v` | Check Maven version (verify installation). |
| `mvn clean` | Deletes the `target/` folder to ensure a fresh build. |
| `mvn compile` | Compiles all project source files (`src/main/java`). |
| `mvn package` | Packages the project into a `.jar` file inside `target/`. |
| `mvn install` | Builds and installs the JAR into your local Maven repository (`~/.m2`). |
| `mvn site` | Generates a project site/documentation if configured. |
| `mvn dependency:tree` | Displays a visual tree of all dependencies (useful for debugging conflicts). |
| `mvn validate` | Checks that the `pom.xml` and project structure are correct. |
| `mvn verify` | Runs integration tests after packaging (if any). |
| `mvn exec:java -Dexec.mainClass="com.vessel.Main"` | Runs your main class directly from Maven. |

---

### 🧹 Useful Tips
- Always run `mvn clean package` after editing `pom.xml` or adding dependencies.
- Use `mvn dependency:tree` to detect version clashes or duplicates.
- If dependencies fail to load, delete `.m2/repository` and rebuild with `mvn clean install`.
- IntelliJ automatically handles Maven imports — you can reload via the “Load Maven Changes” popup anytime.

---
---

### 🚀 Building & Running with IntelliJ IDEA

You don’t need to use terminal commands — IntelliJ can handle Maven automatically.

#### 🧭 Step-by-Step

1. **Ensure Maven is Loaded**
   - On the right sidebar, open the **Maven** tool window.  
   - If it’s hidden, go to **View → Tool Windows → Maven**.  
   - Click the 🔄 **Reload All Maven Projects** icon to sync `pom.xml`.

2. **Build the Project**
   - From the top menu, go to **Build → Build Project** (`Ctrl + F9` / `Cmd + F9`).  
   - This compiles all files in `src/main/java` and places outputs in the `target/` folder.

3. **Run the Application**
   - Open your main class (for example, `Main.java` in `com.vessel`).
   - Click the green ▶️ icon next to the class declaration, or right-click → **Run ‘Main.main()’**.
   - IntelliJ automatically compiles and executes it using Maven dependencies.

4. **Rebuild if Needed**
   - If IntelliJ shows outdated code or dependency issues, select **Build → Rebuild Project**.

---

### 💡 Tips
- If IntelliJ doesn’t recognize new dependencies, click **“Load Maven Changes”** at the top-right.
- To view Maven tasks (clean, package, install): open the **Maven** sidebar → expand your project → run any goal by double-clicking it.
- You can also create a **Run Configuration**:
  - Go to **Run → Edit Configurations → Add New → Application**
  - Set:
    - **Main class:** `com.vessel.Main`
    - **Working directory:** project root
    - **Use classpath of module:** select your module (`vessel`)

---
## Running the App
![img.gif](readme.meta/tut.gif)