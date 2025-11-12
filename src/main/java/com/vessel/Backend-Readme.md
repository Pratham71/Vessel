# Java Notebook Backend — Structure & Git Workflow

## 📦 Package Structure (Final)

```
src/main/java/com/vessel/
├── model/
│     ├── NotebookCell.java
│     ├── Notebook.java
│     └── ExecResult.java
├── kernel/
│     └── NotebookEngine.java
├── persistence/
      └── NotebookPersistence.java

/notebooks/
   <notebook-name>/
        notebook-<timestamp>.json
        classes/
            *.class
```

---

## ✅ File Skeletons (Structure Only)

### `model/NotebookCell.java []`
```java
public class NotebookCell {
    public enum CellType { CODE, MARKDOWN }

    public static class Output {
        public enum Type { STDOUT, STDERR }
        public final Type type;
        public final String text;
    }

    private final String id;
    private CellType type;
    private String content;
    private int executionCount;
    private long lastExecutionTimeMs;
    private List<Output> outputs;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    // TODO:
    // - get/set content
    // - incrementExecutionCount()
    // - addStdout(), addStderr()
    // - clearOutputs()
    // - You can check out Model/NotebookCell.java (I've implemented some code)
}
```

### `model/ExecResult.java`
```java
public record ExecResult(
        int execCount,
        long timeMs,
        List<String> stdout,
        List<String> stderr
) {}
```

### `model/Notebook.java []`
```java
public class Notebook {
    private String name;
    private List<NotebookCell> cells;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    // TODO:
    // - addCell(), removeCell()
    // - getCell(id)
    // - insertCellAt(index, cell)
    // - rename(newName)
    // - touch() update timestamp
    // - 
}
```

### `kernel/NotebookEngine.java [Pratham]`
```java
public class NotebookEngine {

    private JShell shell;
    private Path buildDir;
    private ByteArrayOutputStream outBuf;

    // TODO:
    // - constructor: init persistent JShell + imports
    // - resetKernel()
    // - runCell(cell): detect class/interface/enum/record
    // - compileAndLoad(code)
    // - extractTypeNames(code)
}
```

### `persistence/NotebookPersistence.java [Ramsha]`
```java
public class NotebookPersistence {

    private static final String ROOT = "notebooks/";

    // TODO:
    // - save(notebook)
    // - load(name)
    // - loadOrNew(name)
}
```
---

### `.Json file template.`
```json
{
  "notebook_name": "MyNotebook",
  "metadata": {
    "created_at": "2025-11-10T09:30:00Z",
    "last_modified": "2025-11-10T10:10:00Z",
    "engine_version": "1.0.0"
  },
  "cells": [
    {
      "cell_id": "c1a7a2bf-4a2f-41b2-a788-ec3e53a9811f",
      "cell_type": "CODE",
      "content": "System.out.println(\"Hello\");",
      "execution": {
        "exec_count": 1,
        "time_ms": 23,
        "stdout": [
          "Hello"
        ],
        "stderr": [],
        "history": [
          {
            "exec_count": 1,
            "stdout": "Hello",
            "stderr": "",
            "time_ms": 23
          }
        ]
      },
      "metadata": {
        "title": "Print Hello",
        "created_at": "2025-11-10T09:35:00Z",
        "last_modified": "2025-11-10T09:36:00Z"
      }
    },
    {
      "cell_id": "5dacb120-7103-4cee-a65b-3dd47d5a37df",
      "cell_type": "MARKDOWN",
      "content": "## This is a markdown cell",
      "execution": null,
      "metadata": {
        "title": "Header Text",
        "created_at": "2025-11-10T09:37:00Z",
        "last_modified": "2025-11-10T09:37:00Z"
      }
    }
  ]
}
```
---

## 🧾 Logger Setup (Backend)

### ✅ Logger Configuration (using `java.util.logging`)

### `core/log.java []`
```java
import java.util.logging.Level;
import java.util.logging.Logger;

public class log {

    private static final Logger log = Logger.getLogger("Notebook");

    public static void info(String msg)  { log.log(Level.INFO, msg); }
    public static void warn(String msg)  { log.log(Level.WARNING, msg); }
    public static void error(String msg, Throwable e) { log.log(Level.SEVERE, msg, e); }
}
```


---

## 🔄 Git Workflow (Required)

```
main  ←  pratham71  ←  feat/<feature-name>
```

### Create Feature Branch
```sh
git checkout pratham71
git pull
git checkout -b feat/<feature-name>
```

### Commit + Push
```sh
git add .
git commit -m "feat(kernel): added runCell() structure + detection skeleton"
git push -u origin feat/<feature-name>
```

### Sync With Remote Branch
```sh
git fetch origin
git rebase origin/pratham71
git push --force-with-lease
```

---

## 🏷 Commit Style

| Prefix | Meaning |
|--------|---------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `refactor:` | Changing code structure |
| `docs:` | Documentation updates |
| `chore:` | Dependencies / config changes |

---
