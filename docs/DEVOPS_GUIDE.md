# CampusFix DevOps Guide

Maps every syllabus unit and practical exercise to files in this project.

| Unit / Practical | Where it is demonstrated |
|---|---|
| Unit I – Git, GitHub, cloud (Azure) | Section 1, `azure/provision-vm.sh` |
| Unit II – Maven, POM, lifecycle, profiles, Gradle | `pom.xml` (profiles `fast`, `ci`), `build.gradle` |
| Unit III – Jenkins | `Jenkinsfile`, Section 3 |
| Unit IV – Ansible | `ansible/` (inventory, playbook, roles, ad-hoc commands), Section 4 |
| Unit V – Azure DevOps pipelines | `azure-pipelines.yml`, Section 5 |
| P1 Maven build pipeline in Azure | Build stage of `azure-pipelines.yml` |
| P2 Regression tests in Azure | `Run JUnit tests` step (`mvn test -Pci`), results shown in the *Tests* tab |
| P3 Jenkins in the cloud | Section 3.1 |
| P4 CI pipeline with Jenkins | `Jenkinsfile` stages Checkout → Build → Test → Package → Archive |
| P5 CD pipeline in Jenkins to cloud | `Deploy to Azure VM` stage (`DEPLOY=true`) |
| P6 Ansible playbook for web app infrastructure | `ansible/deploy.yml` + roles |
| P7 Simple app with Gradle | `build.gradle` (`gradle clean build`) |
| P8 Install Ansible, roles, playbooks | Section 4 |

## 1. Git and GitHub
```bash
git init
git add .
git commit -m "feat: initial CampusFix project structure"
git branch -M main
git remote add origin https://github.com/<your-user>/CampusFix.git
git push -u origin main

git checkout -b develop && git push -u origin develop
git checkout -b feature/admin-dashboard          # one branch per feature
git commit -am "feat(admin): add category statistics"
git push -u origin feature/admin-dashboard       # open a Pull Request into develop, review, merge
```
Commit style: `feat:`, `fix:`, `test:`, `docs:`, `ci:`, `chore:`. Never commit `.env`, passwords, `.pem` keys or `uploads/`.

## 2. Maven essentials (Unit II)
* **Lifecycle:** validate → compile → test → package → verify → install → deploy. `mvn package` runs everything before it.
* **Repositories:** local (`~/.m2/repository`), central (Maven Central, default), remote/global (company mirrors in `settings.xml`).
* **Profiles:** `-Pfast` skips tests, `-Pci` adds JaCoCo. **Plugins:** `spring-boot-maven-plugin` (executable JAR), `maven-surefire-plugin` (tests, inherited), `jacoco-maven-plugin`.
* **Dependency management:** versions come from the Spring Boot parent POM; see `mvn dependency:tree`.
* **Gradle:** `gradle clean build`, `gradle test`, `gradle bootRun`, `gradle dependencies`.

## 3. Jenkins (Unit III)
### 3.1 Install Jenkins on an Azure VM (Practical 3)
```bash
# on an Ubuntu 22.04 VM (az vm create ... then ssh in)
sudo apt update && sudo apt install -y openjdk-17-jdk git maven ansible python3-pip
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc >/dev/null
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list
sudo apt update && sudo apt install -y jenkins
sudo systemctl enable --now jenkins
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```
Open port 8080 for Jenkins in the VM's network security group (`az vm open-port -g <rg> -n <vm> --port 8080 --priority 1030`).
If Jenkins shares a VM with the app (which uses 8080), change Jenkins to 8081 in `/lib/systemd/system/jenkins.service` (`Environment="JENKINS_PORT=8081"`).
Browse to `http://<ip>:8080`, paste the password, install suggested plugins, create the admin user.

### 3.2 Plugins and tools
*Manage Jenkins → Plugins:* Git, GitHub, Pipeline, Maven Integration, HTML Publisher, JUnit, Credentials Binding, Copy Artifact, Git Parameter, Extended Choice Parameter.
*Manage Jenkins → Tools:* add **JDK** named `JDK17` (JAVA_HOME `/usr/lib/jvm/java-17-openjdk-amd64`) and **Maven** named `Maven3` (install automatically).

### 3.3 CI pipeline job (Practical 4)
1. *New Item → Pipeline* → name `CampusFix-CI`.
2. *Build Triggers:* tick **GitHub hook trigger for GITScm polling**.
3. *Pipeline → Definition:* *Pipeline script from SCM* → Git → your repo URL → branch `*/main` → Script Path `Jenkinsfile`.
4. GitHub repo → *Settings → Webhooks → Add*: `http://<jenkins-ip>:8080/github-webhook/`, content type `application/json`, event *push*.
5. Push a commit: the job builds, runs the tests (*Test Result* page), publishes the **JaCoCo Coverage** report and archives `target/campusfix.jar`.

**Freestyle alternative:** Source Code Management = Git; Build step *Invoke top-level Maven targets* with goals `clean package`; post-build actions *Publish JUnit test result report* (`target/surefire-reports/*.xml`) and *Archive the artifacts* (`target/*.jar`).

### 3.4 CD pipeline (Practical 5)
Create these credentials (*Manage Jenkins → Credentials*): `azure-vm-ssh` (SSH username with private key), and secret texts `campusfix-db-password`, `campusfix-admin-password`, `azure-vm-host` (the VM public IP).
Run **Build with Parameters** with `DEPLOY = true`: the last stage runs Ansible against the Azure VM using those credentials.
Optional two-job variant: job `CampusFix-Deploy` uses the **Copy Artifact** plugin to fetch `campusfix.jar` from `CampusFix-CI`, then runs `ansible-playbook -i inventory.ini deploy.yml -e jar_src=$WORKSPACE/campusfix.jar`.

## 4. Ansible (Unit IV, Practicals 6 and 8)
Control machine (your laptop/WSL, or the Jenkins VM):
```bash
sudo apt install -y ansible          # or: pip install ansible
cd ansible
ansible-galaxy collection install -r requirements.yml
```
Provision the target VM: `bash azure/provision-vm.sh`, then put its IP into `inventory.ini`.
```bash
ansible -i inventory.ini campusfix -m ping                    # ad-hoc: connectivity
ansible -i inventory.ini campusfix -a "uptime"                # ad-hoc: run a command
ansible -i inventory.ini campusfix -b -m apt -a "name=git state=present"   # ad-hoc: module

export DB_PASSWORD='a-strong-db-password'
export ADMIN_PASSWORD='a-strong-admin-password'
mvn package -DskipTests                                       # produces target/campusfix.jar
ansible-playbook -i inventory.ini deploy.yml --syntax-check
ansible-playbook -i inventory.ini deploy.yml
```
Open `http://<vm-ip>:8080`. Re-running the playbook is safe (idempotent); a new JAR triggers a service restart.

Concepts used: **inventory** (`inventory.ini`), **YAML playbook** (`deploy.yml`), **roles** (`java`, `mysql`, `campusfix`), **modules** (`apt`, `user`, `file`, `copy`, `template`, `systemd`, `uri`, `mysql_db`, `mysql_user`), **handlers** (restart service), **variables** (`group_vars/all.yml`), **templates** (Jinja2 `.j2`).
Master/slave (control node/managed node) setup: the control node only needs SSH access (key-based) to managed nodes – no agent is installed.

## 5. Azure DevOps pipeline (Unit V, Practicals 1 and 2)
1. Create an Azure DevOps organisation and project at <https://dev.azure.com>. (Free tier: request parallelism at <https://aka.ms/azpipelines-parallelism-request> if hosted agents are blocked.)
2. Push the repo to GitHub, then *Pipelines → New pipeline → GitHub → select repo → Existing Azure Pipelines YAML file → `/azure-pipelines.yml`*.
3. *Pipelines → Library → + Variable group* `campusfix-secrets`, add **secret** variables: `VM_HOST`, `VM_USER` (`azureuser`), `DB_PASSWORD`, `ADMIN_PASSWORD`. Authorize the pipeline to use it.
4. *Pipelines → Library → Secure files →* upload the VM private key (`~/.ssh/id_rsa` created by `az vm create --generate-ssh-keys`) as `azure-vm-key.pem`.
5. *Pipelines → Environments → New* `campusfix-prod` (add *Approvals* if you want a manual gate).
6. Run: **Build** stage = checkout, dependency install, compile, JUnit tests (see the *Tests* tab), package, publish artifact `campusfix-drop`. **Deploy** stage (only on `main`) downloads the artifact and runs the Ansible playbook against the VM.
7. Modify `azure-pipelines.yml` (e.g. add a branch to `trigger`, change `-Pci` to `-Pfast`) and watch the pipeline re-run – this is the "modify azure-pipelines.yaml" exercise.

## 6. Azure checklist
* VM created in a resource group, Ubuntu 22.04, ports **22** and **8080** open (NSG rules).
* Database: MySQL on the VM (default, installed by Ansible). For Azure Database for MySQL Flexible Server instead: set `install_mysql: false`, set `db_url`, `db_user`, and allow the VM's IP in the server firewall.
* Public endpoint: `http://<vm-public-ip>:8080`. Health: `/actuator/health`.
* Delete everything when finished to stop charges: `az group delete --name campusfix-rg --yes --no-wait`.

## 7. Secrets policy
Passwords and keys exist only in: Jenkins credentials, Azure DevOps variable group / secure files, and your shell environment.
Ansible writes them to `/etc/campusfix/campusfix.env` (mode 0640) on the VM. Tasks that handle them use `no_log: true`.
If a secret is ever committed, rotate it immediately – deleting the commit is not enough.

## 8. Viva quick answers
* **Why Maven?** Standard lifecycle, dependency management, one command builds the runnable JAR.
* **CI vs CD?** CI = automatically build and test every push; CD = automatically deliver/deploy the tested artifact.
* **Why Ansible?** Agentless, idempotent, declarative YAML; the same playbook rebuilds the server identically.
* **Why test with H2?** Fast, no external database in the pipeline; production uses MySQL.
* **How is access controlled?** Spring Security roles ADMIN/STUDENT, BCrypt hashes, CSRF tokens, ownership checks in `ComplaintService`.
