# ArishDevSecOpsPageLogin

A complete DevSecOps project featuring a login page with Frontend, Backend, Database, and integrated security tools.

## 🚀 Project Overview

This project demonstrates a full DevSecOps pipeline implementation with:
- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java Spring Boot
- **Database**: PostgreSQL
- **DevSecOps Tools**: SonarQube, Trivy, OWASP ZAP
- **Containerization**: Docker
- **Orchestration**: Kubernetes

## 📁 Project Structure

```
arishdevsecopspagelogin/
├── frontend/
│   ├── index.html
│   ├── style.css
│   ├── script.js
│   └── Dockerfile
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/arishdevsecopspagelogin/
│   │       │   ├── LoginApplication.java
│   │       │   ├── model/User.java
│   │       │   ├── repository/UserRepository.java
│   │       │   └── controller/LoginController.java
│   │       └── resources/
│   │           └── application.properties
│   ├── pom.xml
│   └── Dockerfile
├── database/
│   └── init.sql
├── docker-compose.yml
└── README.md
```

## 🛠️ Technologies Used

- **Frontend**: HTML5, CSS3, JavaScript
- **Backend**: Java 17, Spring Boot 3.2.0
- **Database**: PostgreSQL 15
- **Build Tool**: Maven
- **Containerization**: Docker
- **Orchestration**: Kubernetes
- **CI/CD**: Jenkins
- **Security Scanning**: 
  - SonarQube (Code Quality)
  - Trivy (Container Vulnerability Scanning)
  - OWASP ZAP (Security Testing)

## 🔧 Local Setup

### Prerequisites
- Docker & Docker Compose
- Java 17
- Maven
- Git

### Running with Docker Compose

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/arishdevsecopspagelogin.git
cd arishdevsecopspagelogin

# Build and run all services
docker-compose up --build

# Access the application
# Frontend: http://localhost
# Backend API: http://localhost:8080
# Database: localhost:5432
```

### Default Login Credentials

```
Username: admin
Password: admin123

OR

Username: user
Password: pass123
```

## 🔐 DevSecOps Pipeline

The project includes a complete DevSecOps pipeline with:

1. **Code Quality Analysis** (SonarQube)
   - Code smell detection
   - Security vulnerability scanning
   - Code coverage analysis

2. **Container Security** (Trivy)
   - Image vulnerability scanning
   - Dependency checking
   - CVE detection

3. **Application Security** (OWASP ZAP)
   - SQL injection testing
   - XSS detection
   - Security misconfiguration checks

4. **Continuous Integration**
   - Automated builds
   - Unit testing
   - Integration testing

5. **Continuous Deployment**
   - Docker image creation
   - Push to Docker Hub
   - Kubernetes deployment

## 🐳 Docker Images

Build individual images:

```bash
# Frontend
cd frontend
docker build -t arishdevsecopspagelogin-frontend:latest .

# Backend
cd backend
docker build -t arishdevsecopspagelogin-backend:latest .

# Database uses official PostgreSQL image
```

## ☸️ Kubernetes Deployment

```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods
kubectl get services
```

## 📊 API Endpoints

### Login API
```
POST /api/login
Content-Type: application/json

Request Body:
{
  "username": "admin",
  "password": "admin123"
}

Success Response (200):
{
  "message": "Login successful"
}

Error Response (401):
{
  "message": "Invalid credentials"
}
```

## 🔍 Testing

### Manual Testing
1. Access frontend at `http://localhost`
2. Enter credentials
3. Click Login button
4. Check response message

### API Testing with curl
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 📝 Database Schema

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);
```

## 🚦 CI/CD Pipeline Stages

1. **Checkout**: Pull code from GitHub
2. **Build**: Compile Java application
3. **Test**: Run unit tests
4. **SonarQube Analysis**: Code quality check
5. **Docker Build**: Create container images
6. **Trivy Scan**: Scan images for vulnerabilities
7. **Docker Push**: Push to Docker Hub
8. **OWASP ZAP**: Security testing
9. **Deploy**: Deploy to Kubernetes

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

## 👤 Author

**Arish**

## 📞 Support

For issues and questions, please open an issue on GitHub.

---

**Built with ❤️ for DevSecOps learning and demonstration**
