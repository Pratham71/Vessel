## toolbar.md
## overview
this document explains the logic behind the global toolbar in the vessel notebook ui.
the toolbar provides actions that operate on notebook cells such as cut, copy, paste, move up/down, run all, pause, refresh kernel, and theme toggling.

the toolbar is fully synchronized with the notebook model and works together with the global cell selection system.
 ___
## cell selection system

- how selection works:

    - clicking on a cell's root vbox triggers a handler in createCellUI()
    - clearAllSelections() removes selection from all cells 
    - the clicked cell's controller toggles isSelected = true

    - selected cells show a blue highlight border for visual feedback

- related methods (in notebookcontroller):

    - clearAllSelections()
    - getSelectedCell()

- related methods (in genericcellcontroller):
  - setSelected(boolean)
  - isSelected()
- border style updates through setSelected()

## toolbar actions
- cut
    - removes the selected cell from the ui and from the notebook model.
stores it in clipboard for later pasting.

- copy

    - copies selected cell into clipboard without removing it.
clipboard uses a safe deep-copy.

- paste
  - inserts a new cell cloned from clipboard underneath the selected cell.
  updates both ui and model using addCellAt().
- move up
  - swaps the selected cell with the cell above it.
  uses collections.swap for model and ui.
- move down
  - swaps the selected cell with the next one below it.
  same logic as move up but reversed index.

 ## execution actions
- run all

    - executes each code cell in order using the global notebookengine instance.
updates cell output areas using updateOutput() in their controllers.

- pause

    - cancels the run-all future and interrupts the jshell kernel.
- refresh
    - fully resets the kernel, drops variables, clears outputs of all cells, but keeps code intact.

