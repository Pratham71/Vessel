# json save/load implementation - changelog

## overview
added full persistence support for vessel notebooks using gson json serialization.
implemented proper ui → model synchronization, notebook loading, and localdatetime
support. replaced the old placeholder `.vessel` saving mechanism with a complete
notebook-level json system.

---

## added
- notebookpersistence.java (json save/load handler)
    - save(notebook) writes pretty json to /notebooks folder
    - load(name) restores notebook model from disk
    - custom gson adapters for localdatetime
    - filename sanitization + directory creation

- syncModelFromUI() in notebookcontroller
    - collects live ui cells and updates notebook model before saving

- renderNotebook()
    - rebuilds all ui cells from loaded notebook

- setUserData(cellController)
    - allows notebookcontroller to retrieve each cell controller for syncing

- setName() in notebook.java
    - allows renaming without wiping data

---

## changed
- saveProject() now:
    - renames notebook
    - syncs ui state to notebook
    - persists json instead of .vessel format

- addCell() now:
    - adds cell to notebook model immediately

- codecellcontroller:
    - text listener updates notebookcell content
    - cell type updates reflect in model
    - added getNotebookCell()

---

## fixed
- notebooks saving with empty `cells: []`
- content not persisting after restart
- localdatetime gson reflection crash
- ui + notebook model falling out of sync
- loading notebook showing blank cells

---
## updates

## added
- saveToPath() and loadFromPath() in notebookpersistence
  - supports file-explorer–based saving and loading
  - defaults to the /notebooks directory

- setNotebookController() added to codecellcontroller
  - enables child cells to update the active notebook model
## changed
- saveProject() updated
    - now uses system file explorer instead of text input dialog
    - wraps persistence logic in a task



- openProject() updated
    - explorer opens in /notebooks/ by default
    - loads notebook json directly from full path

- delete logic
    - codecellcontroller.deleteCell() now:
        - removes the ui node
        - calls notebook.removecell(id)
        - keeps ui and model in sync


