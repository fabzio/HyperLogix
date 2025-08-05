# HyperLogix

Sistema de Planificación y Simulación de Transporte Logístico
<img width="1855" height="920" alt="image" src="https://github.com/user-attachments/assets/450fc55a-2d78-4624-8e18-fca3e13baff4" />

## Descripción

HyperLogix es una herramienta avanzada diseñada para optimizar y simular operaciones logísticas. Permite a las empresas planificar rutas, gestionar recursos y simular escenarios para mejorar la eficiencia del transporte.

## Características

- **Optimización de Rutas:** Algoritmos avanzados como A* y Colonia de Hormigas para encontrar las rutas más eficientes.
- **Simulación en Tiempo Real:** Simula el comportamiento de camiones, estaciones y pedidos en un entorno dinámico.
- **Gestión de Recursos:** Control de combustible, capacidad de camiones y mantenimiento.
- **Integración con React y TanStack:** Interfaz moderna y reactiva para la visualización de datos.

## Arquitectura del Sistema

### Backend (Java Spring Boot)
- **Puerto:** 8080
- **Base de datos:** PostgreSQL
- **API REST:** Documentada con OpenAPI/Swagger
- **Algoritmos:** A*, Colonia de Hormigas, Algoritmo Genético

### Frontend (React + TypeScript)
- **Puerto:** 5173
- **Framework:** Vite + React + TypeScript
- **Routing:** TanStack Router
- **Estado:** TanStack Query
- **UI:** Tailwind CSS + Shadcn/ui

## Requisitos Previos

### Obligatorios
- **Java:** 21 o superior
- **Maven:** 3.6+ (incluido con el wrapper)
- **PostgreSQL:** 12+ 
- **Node.js:** 18+ o **Bun:** 1.0+

### Herramientas Recomendadas
- **IDE:** IntelliJ IDEA, VS Code
- **Cliente PostgreSQL:** pgAdmin, DBeaver
- **Cliente API:** Postman, Thunder Client

## Instalación y Configuración

### 1. Configuración de Base de Datos

Crea una base de datos PostgreSQL:

```sql
CREATE DATABASE hyperlogix;
CREATE USER hyperlogix_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE hyperlogix TO hyperlogix_user;
```

### 2. Configuración del Backend

1. **Configura las variables de entorno:**

Crea un archivo `.env` en la carpeta `server`:

```env
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hyperlogix
DB_USERNAME=hyperlogix_user
DB_PASSWORD=your_password

# Perfiles de Spring
SPRING_PROFILES_ACTIVE=dev

# Puerto del servidor
SERVER_PORT=8080
```

2. **O configura directamente en `application.yml`:**

```yaml
# server/src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hyperlogix
    username: hyperlogix_user
    password: your_password
```

### 3. Instalación Automática

```bash
# Windows
./develop.ps1

# Linux/macOS
./develop.sh
```

### 4. Instalación Manual

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/fabzio/hyperlogix.git
   cd hyperlogix
   ```

2. **Instala dependencias del proyecto:**
   ```bash
   bun install
   ```

3. **Instala dependencias del backend:**
   ```bash
   cd server
   ./mvnw clean install
   cd ..
   ```

4. **Instala dependencias del frontend:**
   ```bash
   cd client
   bun install
   cd ..
   ```

## Desarrollo

### Ejecución en Modo Desarrollo

#### Opción 1: Comando único (recomendado)
```bash
bun dev
```
Esto iniciará tanto el backend como el frontend simultáneamente.

#### Opción 2: Por separado

**Backend:**
```bash
cd server
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd client
bun start
```

### Acceso a la Aplicación

- **Frontend:** [http://localhost:5173](http://localhost:5173)
- **Backend API:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 Console (dev):** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

## Estructura del Proyecto

```
HyperLogix/
├── server/                 # Backend Java Spring Boot
│   ├── src/main/java/
│   │   └── com/hyperlogix/server/
│   │       ├── config/     # Configuraciones
│   │       ├── domain/     # Entidades de dominio
│   │       ├── features/   # Módulos por funcionalidad
│   │       ├── optimizer/  # Algoritmos de optimización
│   │       └── util/       # Utilidades
│   └── src/main/resources/
├── client/                 # Frontend React TypeScript
│   ├── src/
│   │   ├── api/           # Cliente API generado
│   │   ├── components/    # Componentes reutilizables
│   │   ├── features/      # Módulos por funcionalidad
│   │   ├── hooks/         # Hooks personalizados
│   │   └── routes/        # Rutas de la aplicación
└── docs/                  # Documentación adicional
```

## Scripts Disponibles

### Proyecto Principal
```bash
bun dev              # Desarrollo completo (backend + frontend)
bun test             # Ejecuta todas las pruebas
bun build            # Construye para producción
bun clean            # Limpia archivos generados
```

### Backend
```bash
cd server
./mvnw spring-boot:run    # Ejecuta el servidor
./mvnw test              # Ejecuta pruebas
./mvnw clean package     # Construye JAR
./mvnw clean install     # Instala dependencias
```

### Frontend
```bash
cd client
bun start            # Servidor de desarrollo
bun build            # Construye para producción
bun test             # Ejecuta pruebas con Vitest
bun lint             # Linting con Biome
bun format           # Formateo de código
```

## Generación de API Client

El cliente de API se genera automáticamente desde la especificación OpenAPI del backend:

```bash
cd client
bun run generate-api
```

## Base de Datos

### Perfiles de Desarrollo

- **dev:** PostgreSQL local
- **test:** H2 en memoria
- **prod:** PostgreSQL producción

### Datos de Prueba

Los datos de prueba se cargan automáticamente en el perfil `dev`:
- 20 camiones de diferentes tipos
- 3 estaciones de recarga
- Órdenes de muestra de archivos benchmark

### Migraciones

Las migraciones se manejan automáticamente con Hibernate en desarrollo. Para producción, considera usar Flyway o Liquibase.

## Algoritmos Implementados

### A* (A-Star)
Algoritmo de búsqueda de caminos que encuentra la ruta más corta considerando:
- Obstáculos temporales (roadblocks)
- Consumo de combustible
- Tiempo de viaje

### Colonia de Hormigas (ACO)
Optimización basada en el comportamiento de hormigas para:
- Optimización de rutas múltiples
- Gestión de feromonas
- Exploración vs. explotación

### Algoritmo Genético
Metaheurística evolutiva para:
- Optimización global
- Múltiples objetivos
- Población de soluciones

## API Endpoints Principales

### Camiones
```
GET    /api/v1/trucks          # Lista paginada de camiones
POST   /api/v1/trucks          # Crear camión
PUT    /api/v1/trucks/{id}     # Actualizar camión
DELETE /api/v1/trucks/{id}     # Eliminar camión
```

### Estaciones
```
GET    /api/v1/stations        # Lista de estaciones
POST   /api/v1/stations        # Crear estación
PUT    /api/v1/stations/{id}   # Actualizar estación
```

### Órdenes
```
GET    /api/v1/orders          # Lista de órdenes
POST   /api/v1/orders          # Crear orden
PUT    /api/v1/orders/{id}     # Actualizar orden
```

### Simulación
```
POST   /api/v1/simulation/start/{id}    # Iniciar simulación
POST   /api/v1/simulation/stop/{id}     # Detener simulación
GET    /api/v1/simulation/status/{id}   # Estado de simulación
```

## Pruebas

### Backend
```bash
cd server
./mvnw test                    # Todas las pruebas
./mvnw test -Dtest=NombreTest  # Prueba específica
```

### Frontend
```bash
cd client
bun test                       # Todas las pruebas
bun test -- --watch           # Modo watch
bun test -- --coverage        # Con cobertura
```

## Construcción para Producción

### Backend
```bash
cd server
./mvnw clean package -Pprod
```
Genera: `target/hyperlogix-server-{version}.jar`

### Frontend
```bash
cd client
bun run build
```
Genera archivos en: `dist/`

### Docker (Opcional)
```bash
# Backend
cd server
docker build -t hyperlogix-server .

# Frontend
cd client
docker build -t hyperlogix-client .
```

## Resolución de Problemas Comunes

### 1. Error de conexión a PostgreSQL
- Verifica que PostgreSQL esté ejecutándose
- Confirma las credenciales en `application.yml`
- Asegúrate de que la base de datos existe

### 2. Puerto 8080 ocupado
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/macOS
lsof -ti:8080 | xargs kill -9
```

### 3. Problemas con dependencias de Node
```bash
cd client
rm -rf node_modules
rm bun.lockb
bun install
```

### 4. Error de generación de API
- Asegúrate de que el backend esté ejecutándose
- Verifica que el endpoint OpenAPI esté disponible: `http://localhost:8080/v3/api-docs`

## Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Convenciones de Código

### Backend (Java)
- Usa Google Java Style Guide
- Nombres de métodos en camelCase
- Nombres de clases en PascalCase
- Constantes en UPPER_SNAKE_CASE

### Frontend (TypeScript)
- Usa Biome para linting y formateo
- Componentes en PascalCase
- Hooks en camelCase con prefijo `use`
- Archivos de utlidad en camelCase

## Licencia

Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más detalles.

## Contacto

Para preguntas o soporte, contacta al equipo de desarrollo.
