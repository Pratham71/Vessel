# Simple Guide 
Explaining each component (asked ai to write cuz i am too lazy to type all this)

> note: try to add comments in between code so others can understand easily! #cleancode

last updated: 4/11
### **Main.java**

* Entry point of your app (`Application.launch()`).
* Loads the `main.fxml` layout with `FXMLLoader`.
* Applies `style.css` for basic UI styling.
* Sets up the window (Stage) with size and title.

**Key idea:** This is purely UI initialization — it doesn’t handle any logic yet.

---

### **UIController.java**

* Acts as the **bridge between the FXML UI and your logic**.
* Holds references to UI components via `@FXML` annotations (`VBox notebookArea`, `ListView notebookList`).
* `initialize()` loads existing notebook names into the sidebar.
* `addNewCell()` dynamically loads a new code cell (`codecell.fxml`) and inserts it into the notebook area.
* `saveNotebook()` iterates through all cells and asks `AppLogic` to save them.

**Key idea:** This is your **UI event handler**. Buttons and menu items in FXML call these methods.

---

### **CodeCell.java**

* Represents a **single code cell**, just like in Jupyter.
* Holds a `TextArea` for code and buttons for running/saving.
* Calls `AppLogic.executeJavaCode()` when you click **Run**.
* `getContent()` allows the UIController to fetch cell content for saving.

**Key idea:** Each code cell is its own mini-controller. FXML + controller pattern makes it modular.

---

### **AppLogic.java**

* Handles **core logic**, separated from the UI.
* **File I/O**: saves each cell as a `.md` file inside a folder for each notebook.
* **JShell integration**: executes Java code dynamically and returns output.
* **Notebook management**: lists existing notebooks for the sidebar.

**Key idea:** UI doesn’t deal with execution or saving — all of that is in AppLogic. Clean separation of concerns.

---

### **main.fxml**

* Sets up the **overall layout**:

    * Left: Sidebar (`ListView`) for notebook selection.
    * Center: `VBox` for stacked code cells.
    * Top: Toolbar with **Add Cell** and **Save Notebook** buttons.
* The `fx:id` links the FXML components to `UIController`.

**Key idea:** Layout + event hooks. No logic inside FXML.

---

### **codecell.fxml**

* Defines **a single cell UI**:

    * `TextArea` for code
    * `Run` and `Save` buttons
* Each cell has its own controller (`CodeCell.java`).

**Key idea:** Modular, reusable component — you can add multiple cells dynamically.

---

### **style.css**

* Adds **basic styling**:

    * Padding for notebook area
    * Monospaced font for code
    * Button hover cursor

**Key idea:** Makes the notebook **look professional** without touching logic.

---

### **Flow of the app**

1. **Launch:** `Main.java` → loads `main.fxml` → `UIController.initialize()`.
2. **Sidebar:** `AppLogic.getNotebookNames()` → populates notebook list.
3. **Add Cell:** Toolbar → `UIController.addNewCell()` → loads `codecell.fxml` → injects `CodeCell`.
4. **Run Code:** Each cell’s `Run` button → `CodeCell.runCode()` → JShell executes → output printed.
5. **Save Notebook:** Toolbar → `UIController.saveNotebook()` → `AppLogic.saveCodeCell()` → writes files.

---

This setup is **minimal but modular**, so later you can add:

* Inline outputs per cell (like Jupyter)
* Markdown + code cell support
* Drag-and-drop / reorder cells
* Full notebook file operations (Open, Delete, New)
