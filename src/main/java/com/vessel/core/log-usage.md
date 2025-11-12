    # Logger Usage Guide

This project uses a custom `Log` utility that creates structured log files with
timestamped filenames and module-based directories.

---

## ✅ Getting a Logger

Use `Log.get(<name>)` to obtain a logger instance.

```java
Log engine = Log.get("engine");
Log core = Log.get("core");
Log persistence = Log.get("persistence");
```

Each logger writes to a separate folder:

```
logs/
 ├── engine/
 │    └── engine-2025_11_11-01_22.log
 ├── core/
 │    └── core-2025_11_11-01_22.log
 └── persistence/
      └── persistence-2025_11_11-01_22.log
```

Every run produces a **new log file** with a timestamp in the filename.

---

## ✅ Logging Messages

### Info

```java
engine.info("Engine started");
```

### Warning

```java
engine.warn("Long-running cell execution detected");
```

### Debug

```java
engine.debug("JShell output: " + rawOutput);
```

### Error (with exception)

```java
try {
    saveNotebook();
} catch (Exception e) {
    persistence.error("Failed to save notebook", e);
}
```

---

## ✅ Example Usage

```java
public class NotebookEngine {
    private final Log log = Log.get("engine");

    public void runCell(String code) {
        log.info("Running cell...");
        log.debug("Cell code: " + code);

        try {
            // cell execution logic
        } catch (Exception e) {
            log.error("Execution crashed", e);
        }
    }
}
```

---

## ✅ Log Output Format

```
[2025-11-11 01:22:10] [INFO   ] (NotebookEngine.runCell) -> Running cell...
[2025-11-11 01:22:10] [WARNING] (NotebookEngine.runCell) -> Slow execution detected
[2025-11-11 01:22:11] [SEVERE ] (NotebookEngine.runCell) -> Execution crashed
```

---

## 💡 Quick Reference

| Task                   | Usage                 |
|------------------------|-----------------------|
| Log normal messages    | `log.info("...")`     |
| Log warnings           | `log.warn("...")`     |
| Debug / raw output     | `log.debug("...")`    |
| Config Messages        | `log.config("...")`   |
| Severe Messages        | `log.severe("...")`    |
| Log errors + exception | `log.error("...", e)` |

---

## 🧼 TL;DR

```java
Log log = Log.get("engine");
log.info("done");
```

That's it.
