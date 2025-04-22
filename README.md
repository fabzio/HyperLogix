# HyperLogix

Sistema de Planificación y Simulación de Transporte Logístico

## Descripción

HyperLogix es una herramienta avanzada diseñada para optimizar y simular operaciones logísticas. Permite a las empresas planificar rutas, gestionar recursos y simular escenarios para mejorar la eficiencia del transporte.

## Características

- **Optimización de Rutas:** Algoritmos avanzados como A* y Colonia de Hormigas para encontrar las rutas más eficientes.
- **Simulación en Tiempo Real:** Simula el comportamiento de camiones, estaciones y pedidos en un entorno dinámico.
- **Gestión de Recursos:** Control de combustible, capacidad de camiones y mantenimiento.
- **Integración con React y TanStack:** Interfaz moderna y reactiva para la visualización de datos.

## Requisitos Previos

- **Backend:** Java 21, Maven, PostgreSQL.
- **Frontend:** Node.js o Bun, Vite, React.

## Instalación

### Automática

Ejecuta el script develop.ps1 (Windows) o develop.sh (Linux)

### Manual
1. Clona el repositorio:
   ```bash
   git clone https://github.com/fabzio/hyperlogix.git
   cd hyperlogix
   ```

2. En la carpeta raíz:
```bash
   bun install
```

## Ejecución

En la carpeta raiz
```bash
bun dev
```

Accede a la aplicación en [http://localhost:5173](http://localhost:5173).

## Pruebas

En la carpeta raíz:
```bash
   bun run test
```

## Construcción para Producción

1. Construye el backend:
   ```bash
   cd server
   ./mvnw package
   ```

2. Construye el frontend:
   ```bash
   cd client
   bun run build
   ```
