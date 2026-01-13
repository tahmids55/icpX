# Desktop Directory Features

## Top-Level Features
- Java desktop application (likely JavaFX)
- Maven build system (pom.xml)
- Project configuration (icpX.iml, config.template.json)
- Documentation and reports (022_icpX_ProgressReport_V1.md, IMPLEMENTATION_NOTES.md)
- Build scripts (auto_build.ps1, build_deb.sh)
- Project presentation (icpX_Project_Presentation.pptx)
- Database file (icpx.db)

## src/main
- java/: Main Java source code (package: com.icpx)
- resources/: Application resources

### src/main/java/com/icpx
- Launcher.java, MainApp.java: Application entry points
- controller/: UI and logic controllers
- database/: Database access and helpers
- model/: Data models (Contest, User, etc.)
- service/: Services (Auth, Firebase, Notification, etc.)
- util/: Utility classes
- view/: UI views and FXML files

## target/classes/com/icpx
- Compiled .class files for all modules above
- FXML and resource files for UI
- Credentials and config (desktop_oauth_credentials.json, service-account.json, styles.css)

---

**Key Features:**
- Java desktop app with modular MVC structure
- Maven build and dependency management
- Rich UI with FXML views
- Database integration
- Cloud sync and authentication services
- Extensive documentation and build scripts
