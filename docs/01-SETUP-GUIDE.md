# 🚀 Setup Guide - Movie Booking System

> **Quick Start:** Get the project running in 10 minutes

---

## 📋 PREREQUISITES

### Required Software:
- **Java 17+** (JDK)
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**

### Optional (Recommended):
- **Docker & Docker Compose** - For easy setup
- **Postman** - For API testing
- **IntelliJ IDEA / VSCode** - For development

---

## 🐳 OPTION 1: Docker Compose (Easiest)

### Step 1: Clone Repository

```bash
git clone https://github.com/hoangdinhdung05/Backend-Movie-Booking-System.git
cd Backend-Movie-Booking-System
```

### Step 2: Configure Environment

Create `.env` file (or edit `docker-compose.yml`):

```env
MYSQL_ROOT_PASSWORD=123456789
MYSQL_DATABASE=movie_booking
REDIS_PASSWORD=
```

### Step 3: Start Services

```bash
docker-compose up -d
```

This will start:
- ✅ MySQL on port `3306`
- ✅ Redis on port `6379`
- ✅ Spring Boot app on port `8080`

### Step 4: Verify

```bash
# Check containers
docker ps

# Check API health
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

### Step 5: Access Application

- **API Base:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health

---

## 💻 OPTION 2: Manual Setup

### Step 1: Install Java 17

```bash
# Check Java version
java -version

# Should output: openjdk version "17.x.x"
```

**Download:** https://adoptium.net/temurin/releases/

### Step 2: Install MySQL

#### Windows:
Download installer: https://dev.mysql.com/downloads/installer/

#### macOS:
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

#### Linux:
```bash
sudo apt-get update
sudo apt-get install mysql-server
sudo systemctl start mysql
```

#### Create Database:

```bash
mysql -u root -p

# In MySQL shell:
CREATE DATABASE movie_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### Step 3: Install Redis

#### Windows:
Download: https://github.com/microsoftarchive/redis/releases

#### macOS:
```bash
brew install redis
brew services start redis
```

#### Linux:
```bash
sudo apt-get install redis-server
sudo systemctl start redis
```

#### Verify Redis:

```bash
redis-cli ping
# Expected: PONG
```

### Step 4: Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/movie_booking
    username: root
    password: 123456789  # Change to your MySQL password
  
  data:
    redis:
      host: localhost
      port: 6379
      password:  # Leave empty if no password
  
  mail:
    username: your-email@gmail.com
    password: your-app-password  # Gmail App Password
```

**⚠️ Gmail App Password:**
1. Go to https://myaccount.google.com/apppasswords
2. Generate password for "Mail"
3. Copy to `application.yml`

### Step 5: Build & Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

**Or run JAR directly:**

```bash
java -jar target/movie-booking-system-0.0.1-SNAPSHOT.jar
```

### Step 6: Verify

```bash
# Check API
curl http://localhost:8080/actuator/health

# Check database connection
curl http://localhost:8080/actuator/health/db

# Check Redis connection
curl http://localhost:8080/actuator/health/redis
```

---

## 📊 DATABASE SETUP

### Option A: Auto-create (Development)

Application will auto-create tables on first run:

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update tables
```

### Option B: Manual SQL (Production)

Run migration scripts:

```bash
mysql -u root -p movie_booking < sql/schema.sql
mysql -u root -p movie_booking < sql/seed-data.sql
```

---

## 🧪 VERIFY SETUP

### 1. Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Expected:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 2. Test Registration

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@1234",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "0912345678"
  }'
```

**Expected:** 200 OK + Email sent

### 3. Check Email Logs

```bash
tail -f logs/spring-boot-logger.log | grep "Email sent"
```

---

## 📚 IMPORT SAMPLE DATA

### Option 1: Use Postman Collection

Import `Movie_Booking_System_V1_Collection.postman_collection.json`

Run folder: **"Admin / Seed Data"**

This will create:
- ✅ 10 Movies
- ✅ 3 Theaters
- ✅ 5 Screens
- ✅ 100+ Seats
- ✅ 20 Showtimes

### Option 2: Run SQL Script

```bash
mysql -u root -p movie_booking < sql/seed-data.sql
```

---

## 🔧 CONFIGURATION

### Application Profiles

**Development:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Production:**
```bash
java -jar app.jar --spring.profiles.active=prod
```

### Key Configurations

| Config | Dev | Prod |
|--------|-----|------|
| Log Level | DEBUG | WARN |
| DDL Auto | update | validate |
| Show SQL | true | false |
| JWT Expiry | 30 min | 5 min |
| Refresh Token Expiry | 1 day | 7 days |

---

## 🐛 TROUBLESHOOTING

### Issue: "Connection refused" to MySQL

**Solution:**
```bash
# Check MySQL is running
sudo systemctl status mysql

# Start if not running
sudo systemctl start mysql

# Check port
netstat -an | grep 3306
```

### Issue: "Connection refused" to Redis

**Solution:**
```bash
# Check Redis is running
redis-cli ping

# Start if not running
brew services start redis  # macOS
sudo systemctl start redis  # Linux
```

### Issue: "Table doesn't exist"

**Solution:**
```yaml
# Set DDL auto to create
spring:
  jpa:
    hibernate:
      ddl-auto: create  # Or run SQL script manually
```

### Issue: Email not sending

**Check:**
- ✅ Gmail App Password correct?
- ✅ Less secure apps enabled? (Not needed if using App Password)
- ✅ SMTP config correct?

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # NOT your login password!
```

### Issue: Port 8080 already in use

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process or change port
server.port=8081  # application.yml
```

---

## 📦 PROJECT STRUCTURE

```
Backend-Movie-Booking-System/
├── src/
│   ├── main/
│   │   ├── java/com/trainning/movie_booking_system/
│   │   │   ├── config/           # Spring configurations
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── service/          # Business logic
│   │   │   ├── repository/       # Data access
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── dto/              # Request/Response DTOs
│   │   │   ├── security/         # JWT & Security
│   │   │   ├── exception/        # Exception handling
│   │   │   └── helper/           # Utilities
│   │   └── resources/
│   │       ├── application.yml   # Main config
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/                     # Unit & integration tests
├── docs/                         # Documentation
├── logs/                         # Application logs
├── docker-compose.yml            # Docker setup
├── pom.xml                       # Maven dependencies
└── README.md                     # Project overview
```

---

## 🎯 NEXT STEPS

After successful setup:

1. ✅ Read [API Documentation](02-API-DOCUMENTATION.md) to understand endpoints
2. ✅ Import Postman collection for testing
3. ✅ Read [Booking Flow](04-BOOKING-FLOW.md) to understand business logic
4. ✅ Read [Payment Flow](05-PAYMENT-FLOW.md) for payment integration
5. ✅ Run [Testing Guide](07-TESTING-GUIDE.md) test scenarios

---

## 📞 SUPPORT

**Issues?** Open a ticket at: https://github.com/hoangdinhdung05/Backend-Movie-Booking-System/issues

---

**🎉 Happy Coding!**
