# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.2/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.2/maven-plugin/build-image.html)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

# 📘 Backend Setup Guide – GenAI Automation (Spring Boot 3.x, Java 17)

## 📖 Overview
This backend powers the **GenAI Automation** application, offering AI-assisted tools for:
- **Flaky Test Analyzer** – Analyze test run results and identify flaky tests.
- **Jira Story to TestCase** – Generate BDD/TDD test cases from Jira stories.
- **Swagger to RestAssured** – Convert Swagger API specs to RestAssured test code.
- **Mobile Automation Code Generator** – Generate Appium/Selenium-based POM classes from mobile DOM dumps.
- **Selenium to Playwright Converter** – Convert Selenium test code to Playwright.

It is built using **Spring Boot 3.x** and exposes REST APIs consumed by the frontend.

## 🛠 Prerequisites
Before running this backend, ensure you have:

| Tool | Version | Download Link |
|------|---------|--------------|
| **Java** | 17+ | [Download Java 17](https://adoptium.net/) |
| **Maven** | 3.8+ | [Download Maven](https://maven.apache.org/download.cgi) |
| **Spring Boot** | 3.x | Installed via Maven |
| **Git** | Latest | [Download Git](https://git-scm.com/downloads) |


## 📦 Step 1: Install Java 17

### **Windows**
1. Download and install Java 17 from [Adoptium](https://adoptium.net/temurin/releases/?version=17).
2. Open **Command Prompt** and verify:
   java -version

Expected output:
openjdk version "17.x.x"

### **macOS**

1. Install Java 17 using **Homebrew**:

   brew install openjdk@17
   
2. Link it:

   
   sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
   
3. Verify:

   
   java -version

## 📦 Step 2: Install Maven

### **Windows**

1. Download Maven from [here](https://maven.apache.org/download.cgi).
2. Extract it to `C:\Program Files\Apache\Maven`.
3. Add Maven’s `bin` folder to **Environment Variables** → `PATH`.
4. Verify:

   
   mvn -version
   

### **macOS**


brew install maven
mvn -version

## 📦 Step 3: Setup Spring Boot 3.x

You don’t install Spring Boot separately — it’s included in your Maven project.

To create a new Spring Boot project (if needed):


mvn archetype:generate -DgroupId=com.example -DartifactId=genai-backend \
    -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

Or use **Spring Initializr**:

* Go to [https://start.spring.io](https://start.spring.io)
* Select:

    * Project: Maven
    * Language: Java
    * Spring Boot: **3.x.x**
    * Dependencies: **Spring Web**, **Spring Boot DevTools**
* Download and extract.

---

## 📦 Step 4: Clone the Repository


## 📦 Step 5: Build & Run

mvn clean install
mvn spring-boot:run


The backend will start at:


http://localhost:8080


---

## 🔗 API Endpoints Overview

| Feature                | Endpoint                          | Method |
| ---------------------- | --------------------------------- | ------ |
| Flaky Test Analyzer    | `/flaky/analyze`                  | POST   |
| Download Sample CSV    | `/flaky/sample/csv`               | GET    |
| Download Sample JSON   | `/flaky/sample/json`              | GET    |
| Jira Story to TestCase | `/jira/generate`                  | POST   |
| Swagger to RestAssured | `/swagger/generate`               | POST   |
| Mobile Automation Code | `/mobile/generate`                | POST   |
| Selenium to Playwright | `/convert/selenium-to-playwright` | POST   |

---

## 📂 Project Structure

```
src/
 ├── main/
 │   ├── java/testleaf/controller/     # REST Controllers
 │   ├── java/testleaf/llm/            # Core LLM processing logic
 │   ├── resources/static/templates/   # Sample CSV/JSON files
 │   ├── resources/application.yml     # Configurations
 └── test/                             # Unit tests
```

---

## 🚀 Usage with Frontend

1. Start the backend (`mvn spring-boot:run`).
2. Start the React frontend (see **README\_FE.md**).
3. Access the app in your browser.
4. Select a module (e.g., Flaky Test Analyzer) and interact with the UI.

---

## 🛠 Troubleshooting

| Problem                            | Solution                                                         |
| ---------------------------------- | ---------------------------------------------------------------- |
| `java: source release 17 required` | Ensure Java 17 is installed & set as default.                    |
| `Address already in use: 8080`     | Stop the process using 8080 or change port in `application.yml`. |
| Maven not found                    | Add Maven `bin` to `PATH`.                                       |
| CSV/JSON parsing errors            | Use provided **sample files** from `/static/templates/`.         |

