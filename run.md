# How to Run Vessel

To run the application, open your terminal in this directory (`Vessel`) and run:

```bash
mvn clean javafx:run
```

JUST TYPE `.\run.ps1`
## Prerequisites
- Maven installed
- Java 17 or higher (configured in `pom.xml`)

## Troubleshooting
If you encounter "invalid target release" errors, ensure your `JAVA_HOME` is set to JDK 17 or higher and that the `pom.xml` compiler source/target matches your JDK version.
