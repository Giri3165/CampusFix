# CampusFix – Smart Campus Complaint Management System

Students report campus maintenance problems (broken fans, leaks, Wi-Fi, lab equipment…); administrators assign
staff, update status and track everything on a dashboard. Built as a **DevOps mini-project**: Spring Boot app +
Git/GitHub, Maven (and Gradle), Jenkins, Ansible and Azure DevOps.

| Layer | Technology |
|---|---|
| Frontend | Thymeleaf templates, HTML5, CSS3, Bootstrap 5, JavaScript |
| Backend | Java 17, Spring Boot 3.2 (MVC, Data JPA, Security) |
| Database | MySQL 8 (H2 in-memory for automated tests) |
| Build | Maven (`pom.xml`), Gradle (`build.gradle`) |
| Tests | JUnit 5, Mockito, Spring MockMvc |
| DevOps | Git/GitHub, Jenkins, Ansible, Azure VM, Azure DevOps Pipelines |

## 1. Run locally (about 5 minutes)

**Prerequisites:** JDK 17+, Maven 3.9+, MySQL 8 running locally, Git.

```bash
# 1. create the database user (or use root)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS campusfix;
  CREATE USER IF NOT EXISTS 'campusfix'@'localhost' IDENTIFIED BY 'change-me';
  GRANT ALL ON campusfix.* TO 'campusfix'@'localhost';"

# 2. configure through environment variables (nothing secret lives in the repo)
export DB_USERNAME=campusfix
export DB_PASSWORD=change-me
export ADMIN_EMAIL=admin@campusfix.local     # first administrator, created automatically
export ADMIN_PASSWORD='ChooseAStrongOne1!'

# 3. run
mvn spring-boot:run
```
Open <http://localhost:8080>. Tables are created automatically (`database/schema.sql` is provided if you prefer to
create them by hand). Windows PowerShell: use `$env:DB_PASSWORD="change-me"` instead of `export`.

**Accounts**
* **Administrator:** the `ADMIN_EMAIL` / `ADMIN_PASSWORD` you exported (defaults `admin@campusfix.local` / `ChangeMe@123` – change them).
* **Student:** use *Register* on the landing page.
* First admin task: **Maintenance staff → add staff**, so complaints can be assigned.

No fake data is inserted: every number on the dashboards comes from the database.

## 2. Features
* **Student:** register/login, submit complaints with category, priority, location and photo (JPG/PNG/GIF ≤ 5 MB, validated by extension *and* real image content), search/filter, pagination, status badges, admin remarks, timeline, edit/cancel while *Submitted* or *Acknowledged*.
* **Administrator:** live totals (total, pending, in progress, resolved), category analytics, filter by category/priority/status, assign staff, change status, resolution remarks (mandatory for Resolved/Rejected), staff management, complete history per complaint.
* **Security:** BCrypt password hashing, role-based access (`/admin/**` ADMIN only, `/student/**` STUDENT only), CSRF protection, students can only open their own complaints, uploads served only to logged-in users.

Status flow: Submitted → Acknowledged → Assigned → In Progress → Resolved (or Rejected).

## 3. Build, test, package

```bash
mvn clean compile            # compile
mvn test                     # JUnit 5 + Mockito + MockMvc (uses H2, no MySQL needed)
mvn package                  # -> target/campusfix.jar (runnable)
mvn install                  # also installs into ~/.m2
mvn verify -Pci              # tests + JaCoCo coverage report in target/site/jacoco
mvn package -Pfast           # skip tests (quick local build)
java -jar target/campusfix.jar
```
Gradle equivalent: `gradle clean build` and `gradle bootRun` (jar at `build/libs/campusfix.jar`).

Test coverage: registration, authentication (success/failure/role redirect), complaint submission and validation,
assignment, status updates, student edit/cancel rules, role-based access control, file upload validation.

## 4. Project structure
```
CampusFix/
├── pom.xml  build.gradle  settings.gradle
├── Jenkinsfile                    # CI/CD pipeline for Jenkins
├── azure-pipelines.yml            # Azure DevOps pipeline
├── ansible/                       # inventory.ini, deploy.yml, group_vars, roles/{java,mysql,campusfix}
├── azure/provision-vm.sh          # creates the Azure VM + opens ports with az CLI
├── database/schema.sql
├── docs/DEVOPS_GUIDE.md           # step-by-step Jenkins / Ansible / Azure setup + viva notes
├── src/main/java/com/campusfix/   # model, repository, service, controller, config, dto
├── src/main/resources/            # application.properties, templates/, static/
└── src/test/                      # unit + integration tests
```

## 5. DevOps workflow (summary – full steps in `docs/DEVOPS_GUIDE.md`)
1. **Git/GitHub:** `main` (stable) and `develop`; feature branches such as `feature/complaint-filters`; pull request → merge.
2. **Jenkins:** GitHub push → `Jenkinsfile` runs checkout → compile → test (JUnit report + JaCoCo) → package → archive `campusfix.jar` → optional deploy.
3. **Ansible:** `ansible-playbook -i inventory.ini deploy.yml` installs Java and MySQL, copies the JAR, writes a systemd service and runs a health check.
4. **Azure:** `bash azure/provision-vm.sh` creates the Ubuntu VM and opens port 8080; the app is reachable at `http://<VM-IP>:8080`.
5. **Azure DevOps:** `azure-pipelines.yml` builds, tests, publishes the artifact and deploys to the VM through Ansible. Secrets live in a variable group and secure file, never in Git.

## 6. Configuration reference
| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC URL | local MySQL `campusfix` |
| `DB_USERNAME` / `DB_PASSWORD` | database login | `root` / empty |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | first administrator | `admin@campusfix.local` / `ChangeMe@123` |
| `UPLOAD_DIR` | where images are stored | `uploads` |
| `PORT` | HTTP port | `8080` |
| `DDL_AUTO` | Hibernate schema mode | `update` |

Copy `.env.example` as a template. `.env`, `*.pem` and `uploads/` are in `.gitignore`.

## 7. Troubleshooting
* *Access denied for user* – check `DB_USERNAME`/`DB_PASSWORD` and that MySQL is running.
* *Port 8080 in use* – `export PORT=9090`.
* *Cannot reach the Azure app* – open port 8080 in the VM's Network Security Group (`azure/provision-vm.sh` does this).
* *Lombok errors in the IDE* – install the Lombok plugin and enable annotation processing (Maven builds work without extra setup).
