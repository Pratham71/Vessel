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
![img.gif]()

# 2.1 Adding Maven to path
![img.gif]()

---

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

![img.png](misc_images/git_status.png)

### Check Git Status

* Open the **Git** tool window at the bottom (or use `Alt + 9`/ `Ctrl + Shift + G` / `Cmd + 9`).
* You’ll see all modified files, main commits and branched commits listed.

---

![img.png](misc_images/git_branch_menu.png)

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

### Add Pom.xml
