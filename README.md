# GTPLUG Underground Work Management System

A production-ready full-stack web application for managing underground (UG) cable installation work assigned by administrators to vendors.

## Features

### Authentication & Security
- JWT-based authentication with HTTPOnly cookies
- BCrypt password hashing
- CSRF protection
- Rate limiting on login endpoints
- Account lock after repeated failed logins
- XSS protection and input sanitization

### User Roles
- **Admin**: Full access to manage vendors, assign projects, track progress, approve material requests
- **Vendor**: View assigned work, submit daily progress updates, request materials

### Core Functionality
- Vendor registration and management
- Project assignment with location tracking
- Daily work progress updates with photo proof
- Material request system
- Real-time progress tracking
- Activity logging
- Notification system

### Technical Features
- Progressive Web App (PWA) support
- Responsive design for mobile, tablet, and desktop
- Docker containerization
- Database connection pooling (HikariCP)
- MySQL database with automatic schema initialization

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Javalin 5.6 |
| Frontend | Thymeleaf, TailwindCSS, Vanilla JS |
| Database | MySQL 8.0 |
| Build Tool | Gradle |
| Containerization | Docker, Docker Compose |
| Security | JWT, BCrypt, CSRF |

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17 (for local development)
- Gradle 8.x (for local development)

### Running with Docker

1. Clone the repository:
```bash
cd gtplug-ug-work-system
```

2. Start the application:
```bash
docker-compose up -d
```

3. Access the application:
- Web: http://localhost:8080
- Default Admin: `admin` / `admin123`
- Default Vendor: `vendor1` / `vendor123`

### Stopping the Application

```bash
docker-compose down
```

To remove all data (including database):
```bash
docker-compose down -v
```

## Development Setup

### Local Development

1. Start MySQL database:
```bash
docker run -d \
  --name gtplug-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=gtplug_db \
  -p 3306:3306 \
  mysql:8.0
```

2. Build and run the application:
```bash
./gradlew shadowJar
java -jar build/libs/gtplug-ug-work-system.jar
```

Or use Gradle to run:
```bash
./gradlew run
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | localhost |
| `DB_PORT` | Database port | 3306 |
| `DB_NAME` | Database name | gtplug_db |
| `DB_USER` | Database user | root |
| `DB_PASSWORD` | Database password | root |
| `JWT_SECRET` | JWT signing secret | (generated) |
| `JWT_EXPIRATION` | JWT expiration (ms) | 86400000 |
| `ENVIRONMENT` | dev/prod | development |

## Project Structure

```
src/main/
├── java/com/gtplug/
│   ├── Application.java          # Main entry point
│   ├── config/                   # Configuration classes
│   ├── controllers/              # HTTP request handlers
│   ├── models/                   # Entity classes
│   ├── repositories/             # Database access layer
│   ├── services/                 # Business logic
│   ├── security/                 # JWT, Password utils
│   └── middleware/               # Auth, CSRF, Rate limiting
├── resources/
│   ├── templates/                # Thymeleaf templates
│   │   ├── admin/               # Admin pages
│   │   ├── vendor/              # Vendor pages
│   │   ├── auth/                # Login/Signup pages
│   │   └── error/               # Error pages
│   ├── static/                  # CSS, JS, images
│   ├── db/init/                 # Database schema
│   └── application.properties   # Configuration
```

## API Endpoints

### Public Endpoints
- `GET /` - Home page
- `GET /login` - Login page
- `POST /login` - Authenticate
- `GET /vendor/signup` - Vendor registration page
- `POST /vendor/signup` - Register vendor
- `GET /logout` - Logout

### Admin Endpoints (Requires Admin Role)
- `GET /admin/dashboard` - Admin dashboard
- `GET /admin/vendors` - Vendor catalog
- `GET /admin/assign-work` - Assign work form
- `POST /admin/assign-work` - Create project
- `GET /admin/projects` - Project tracking
- `GET /admin/material-requests` - Material requests
- `GET /admin/logs` - Activity logs

### Vendor Endpoints (Requires Vendor Role)
- `GET /vendor/dashboard` - Vendor dashboard
- `GET /vendor/projects` - Assigned projects
- `GET /vendor/daily-update` - Daily update form
- `POST /vendor/daily-update` - Submit update
- `GET /vendor/material-request` - Material request form
- `POST /vendor/material-request` - Submit request

## Security Features

1. **Password Security**
   - BCrypt hashing with configurable rounds
   - Strong password validation
   - Account lock after 5 failed attempts

2. **Session Management**
   - JWT tokens with expiration
   - HTTPOnly cookies
   - Secure session handling

3. **Request Protection**
   - CSRF tokens for state-changing operations
   - Rate limiting on sensitive endpoints
   - Input validation and sanitization

4. **Database Security**
   - Prepared statements (SQL injection prevention)
   - Connection pooling
   - Secure credential management

## Database Schema

### Core Tables
- `users` - Authentication data
- `vendors` - Vendor profiles
- `admins` - Admin profiles
- `projects` - Project assignments
- `daily_updates` - Work progress updates
- `material_requests` - Material requests
- `admin_logs` - Activity audit trail
- `notifications` - User notifications

## PWA Features

- Installable on mobile home screen
- Offline support with service worker
- App manifest for native-like experience
- Responsive design for all devices

## Troubleshooting

### Database Connection Issues
```bash
# Check MySQL container logs
docker logs gtplug-mysql

# Reset database
docker-compose down -v
docker-compose up -d
```

### Build Issues
```bash
# Clean build
./gradlew clean shadowJar

# Check dependencies
./gradlew dependencies
```

## License

This project is proprietary software for GTPLUG.

## Support

For support, contact the development team.
