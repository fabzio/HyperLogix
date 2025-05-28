Welcome to your new TanStack app! 

# Getting Started

To run this application:

```bash
bun install
bun run start  
```

# Building For Production

To build this application for production:

```bash
bun run build
```

## Testing

This project uses [Vitest](https://vitest.dev/) for testing. You can run the tests with:

```bash
bun run test
```

## Styling

This project uses [Tailwind CSS](https://tailwindcss.com/) for styling.


## Linting & Formatting

This project uses [Biome](https://biomejs.dev/) for linting and formatting. The following scripts are available:


```bash
bun run lint
bun run format
bun run check
```


## Shadcn

Add components using the latest version of [Shadcn](https://ui.shadcn.com/).

```bash
pnpx shadcn@latest add button
```


## T3Env

- You can use T3Env to add type safety to your environment variables.
- Add Environment variables to the `src/env.mjs` file.
- Use the environment variables in your code.

### Usage

```ts
import { env } from "@/env";

console.log(env.VITE_APP_TITLE);
```






## Routing
This project uses [TanStack Router](https://tanstack.com/router). The initial setup is a file based router. Which means that the routes are managed as files in `src/routes`.

### Adding A Route

To add a new route to your application just add another a new file in the `./src/routes` directory.

TanStack will automatically generate the content of the route file for you.

Now that you have two routes you can use a `Link` component to navigate between them.

### Adding Links

To use SPA (Single Page Application) navigation you will need to import the `Link` component from `@tanstack/react-router`.

```tsx
import { Link } from "@tanstack/react-router";
```

Then anywhere in your JSX you can use it like so:

```tsx
<Link to="/about">About</Link>
```

This will create a link that will navigate to the `/about` route.

More information on the `Link` component can be found in the [Link documentation](https://tanstack.com/router/v1/docs/framework/react/api/router/linkComponent).

### Using A Layout

In the File Based Routing setup the layout is located in `src/routes/__root.tsx`. Anything you add to the root route will appear in all the routes. The route content will appear in the JSX where you use the `<Outlet />` component.

Here is an example layout that includes a header:

```tsx
import { Outlet, createRootRoute } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'

import { Link } from "@tanstack/react-router";

export const Route = createRootRoute({
  component: () => (
    <>
      <header>
        <nav>
          <Link to="/">Home</Link>
          <Link to="/about">About</Link>
        </nav>
      </header>
      <Outlet />
      <TanStackRouterDevtools />
    </>
  ),
})
```

The `<TanStackRouterDevtools />` component is not required so you can remove it if you don't want it in your layout.

More information on layouts can be found in the [Layouts documentation](https://tanstack.com/router/latest/docs/framework/react/guide/routing-concepts#layouts).


## Data Fetching

There are multiple ways to fetch data in your application. You can use TanStack Query to fetch data from a server. But you can also use the `loader` functionality built into TanStack Router to load the data for a route before it's rendered.

For example:

```tsx
const peopleRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/people",
  loader: async () => {
    const response = await fetch("https://swapi.dev/api/people");
    return response.json() as Promise<{
      results: {
        name: string;
      }[];
    }>;
  },
  component: () => {
    const data = peopleRoute.useLoaderData();
    return (
      <ul>
        {data.results.map((person) => (
          <li key={person.name}>{person.name}</li>
        ))}
      </ul>
    );
  },
});
```

Loaders simplify your data fetching logic dramatically. Check out more information in the [Loader documentation](https://tanstack.com/router/latest/docs/framework/react/guide/data-loading#loader-parameters).

### React-Query

React-Query is an excellent addition or alternative to route loading and integrating it into you application is a breeze.

First add your dependencies:

```bash
bun install @tanstack/react-query @tanstack/react-query-devtools
```

Next we'll need to create a query client and provider. We recommend putting those in `main.tsx`.

```tsx
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// ...

const queryClient = new QueryClient();

// ...

if (!rootElement.innerHTML) {
  const root = ReactDOM.createRoot(rootElement);

  root.render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
```

You can also add TanStack Query Devtools to the root route (optional).

```tsx
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

const rootRoute = createRootRoute({
  component: () => (
    <>
      <Outlet />
      <ReactQueryDevtools buttonPosition="top-right" />
      <TanStackRouterDevtools />
    </>
  ),
});
```

Now you can use `useQuery` to fetch your data.

```tsx
import { useQuery } from "@tanstack/react-query";

import "./App.css";

function App() {
  const { data } = useQuery({
    queryKey: ["people"],
    queryFn: () =>
      fetch("https://swapi.dev/api/people")
        .then((res) => res.json())
        .then((data) => data.results as { name: string }[]),
    initialData: [],
  });

  return (
    <div>
      <ul>
        {data.map((person) => (
          <li key={person.name}>{person.name}</li>
        ))}
      </ul>
    </div>
  );
}

export default App;
```

You can find out everything you need to know on how to use React-Query in the [React-Query documentation](https://tanstack.com/query/latest/docs/framework/react/overview).

## State Management

Another common requirement for React applications is state management. There are many options for state management in React. TanStack Store provides a great starting point for your project.

First you need to add TanStack Store as a dependency:

```bash
bun install @tanstack/store
```

Now let's create a simple counter in the `src/App.tsx` file as a demonstration.

```tsx
import { useStore } from "@tanstack/react-store";
import { Store } from "@tanstack/store";
import "./App.css";

const countStore = new Store(0);

function App() {
  const count = useStore(countStore);
  return (
    <div>
      <button onClick={() => countStore.setState((n) => n + 1)}>
        Increment - {count}
      </button>
    </div>
  );
}

export default App;
```

One of the many nice features of TanStack Store is the ability to derive state from other state. That derived state will update when the base state updates.

Let's check this out by doubling the count using derived state.

```tsx
import { useStore } from "@tanstack/react-store";
import { Store, Derived } from "@tanstack/store";
import "./App.css";

const countStore = new Store(0);

const doubledStore = new Derived({
  fn: () => countStore.state * 2,
  deps: [countStore],
});
doubledStore.mount();

function App() {
  const count = useStore(countStore);
  const doubledCount = useStore(doubledStore);

  return (
    <div>
      <button onClick={() => countStore.setState((n) => n + 1)}>
        Increment - {count}
      </button>
      <div>Doubled - {doubledCount}</div>
    </div>
  );
}

export default App;
```

We use the `Derived` class to create a new store that is derived from another store. The `Derived` class has a `mount` method that will start the derived store updating.

Once we've created the derived store we can use it in the `App` component just like we would any other store using the `useStore` hook.

You can find out everything you need to know on how to use TanStack Store in the [TanStack Store documentation](https://tanstack.com/store/latest).

# HyperLogix Frontend

Frontend de React con TypeScript para el sistema de planificación logística HyperLogix.

## Stack Tecnológico

- **React 18** - Biblioteca de UI
- **TypeScript** - Tipado estático
- **Vite** - Build tool y dev server
- **TanStack Router** - Routing declarativo
- **TanStack Query** - Gestión de estado servidor
- **Tailwind CSS** - Framework de estilos
- **Shadcn/ui** - Componentes de UI
- **Biome** - Linting y formateo
- **Vitest** - Framework de testing

## Inicio Rápido

```bash
# Instalar dependencias
bun install

# Servidor de desarrollo
bun start

# Construir para producción
bun build

# Ejecutar pruebas
bun test
```

## Scripts Disponibles

```bash
bun start           # Servidor de desarrollo (puerto 5173)
bun build          # Construir para producción
bun preview        # Preview de la build de producción
bun test           # Ejecutar pruebas con Vitest
bun test:watch     # Pruebas en modo watch
bun test:coverage  # Pruebas con reporte de cobertura
bun lint           # Linting con Biome
bun format         # Formatear código
bun check          # Verificar tipos TypeScript
bun generate-api   # Generar cliente API desde OpenAPI
```

## Estructura del Proyecto

```
src/
├── api/                 # Cliente API auto-generado
│   ├── api.ts          # Interfaces y clases API
│   ├── base.ts         # Configuración base
│   └── docs/           # Documentación API
├── components/         # Componentes reutilizables
│   ├── ui/            # Componentes base (Shadcn)
│   └── EntityManagement.tsx
├── features/          # Módulos por funcionalidad
│   ├── dashboard/     # Dashboard principal
│   ├── trucks/        # Gestión de camiones
│   ├── stations/      # Gestión de estaciones
│   └── simulation/    # Sistema de simulación
├── hooks/             # Hooks personalizados
│   ├── useFilters.ts  # Hook para filtros
│   └── useTrucks.ts   # Hook para camiones
├── layouts/           # Layouts de la aplicación
│   ├── PageLayout.tsx # Layout base de páginas
│   └── Sidebar/       # Componentes de navegación
├── routes/            # Definición de rutas (TanStack Router)
│   ├── __root.tsx     # Ruta raíz
│   ├── index.tsx      # Página principal
│   └── _auth/         # Rutas autenticadas
└── lib/               # Utilidades y configuraciones
    └── utils.ts       # Funciones helper
```

## Arquitectura de Componentes

### Componentes de UI Base (Shadcn)
Los componentes básicos están en `src/components/ui/` y siguen el patrón de Shadcn:

```tsx
// Ejemplo de uso
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'

function MyComponent() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Título</CardTitle>
      </CardHeader>
      <Button variant="outline">Acción</Button>
    </Card>
  )
}
```

### Features (Módulos)
Cada feature es autocontenida con su propia estructura:

```
features/trucks/
├── index.tsx          # Componente principal
├── components/        # Componentes específicos
├── hooks/            # Hooks del módulo
├── columns.tsx       # Definición de columnas tabla
└── types.ts          # Tipos TypeScript
```

### Gestión de Estado

#### TanStack Query para datos del servidor:
```tsx
import { useQuery } from '@tanstack/react-query'
import { TruckControllerApi } from '@/api'

function useTrucks(filters: any) {
  return useQuery({
    queryKey: ['trucks', filters],
    queryFn: () => new TruckControllerApi().list(filters),
    select: (data) => data.data.content
  })
}
```

#### Estado local con hooks:
```tsx
import { useState, useCallback } from 'react'

function useFilters(initialState: any) {
  const [filters, setFilters] = useState(initialState)
  
  const updateFilter = useCallback((key: string, value: any) => {
    setFilters(prev => ({ ...prev, [key]: value }))
  }, [])
  
  return { filters, updateFilter, setFilters }
}
```

## Routing con TanStack Router

### Definición de Rutas
Las rutas se definen como archivos en `src/routes/`:

```tsx
// src/routes/trucks.tsx
import { createFileRoute } from '@tanstack/react-router'
import TrucksFeature from '@/features/trucks'

export const Route = createFileRoute('/trucks')({
  component: TrucksFeature
})
```

### Navegación
```tsx
import { Link, useNavigate } from '@tanstack/react-router'

function Navigation() {
  const navigate = useNavigate()
  
  return (
    <nav>
      <Link to="/trucks">Camiones</Link>
      <Link to="/stations">Estaciones</Link>
      <button onClick={() => navigate({ to: '/dashboard' })}>
        Dashboard
      </button>
    </nav>
  )
}
```

## API Client

### Generación Automática
El cliente API se genera automáticamente desde el backend:

```bash
bun run generate-api
```

Esto crea las interfaces TypeScript en `src/api/` basadas en la especificación OpenAPI.

### Uso del Cliente API
```tsx
import { TruckControllerApi, StationControllerApi } from '@/api'

// Instanciar APIs
const truckApi = new TruckControllerApi()
const stationApi = new StationControllerApi()

// Usar con TanStack Query
const { data: trucks } = useQuery({
  queryKey: ['trucks'],
  queryFn: () => truckApi.list({ page: 0, size: 20 })
})
```

### Configuración de Base URL
```tsx
// src/api/configuration.ts
export const apiConfig = new Configuration({
  basePath: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
})
```

## Estilos con Tailwind CSS

### Configuración
El proyecto usa Tailwind CSS con configuración personalizada en `tailwind.config.js`.

### Patrones Comunes
```tsx
// Layout containers
<div className="container mx-auto px-4 py-8">

// Cards
<div className="bg-white rounded-lg shadow-md p-6">

// Buttons
<button className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">

// Forms
<input className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
```

### Variables CSS Personalizadas
```css
/* src/index.css */
:root {
  --primary: 222.2 84% 4.9%;
  --primary-foreground: 210 40% 98%;
  --secondary: 210 40% 96%;
  --accent: 210 40% 96%;
}
```

## Testing con Vitest

### Configuración
El testing está configurado con Vitest en `vitest.config.ts`.

### Ejemplos de Pruebas
```tsx
// tests/components/Button.test.tsx
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Button } from '@/components/ui/button'

describe('Button', () => {
  it('renders with text', () => {
    render(<Button>Click me</Button>)
    expect(screen.getByRole('button')).toHaveTextContent('Click me')
  })
})
```

### Testing de Hooks
```tsx
// tests/hooks/useFilters.test.ts
import { renderHook, act } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useFilters } from '@/hooks/useFilters'

describe('useFilters', () => {
  it('updates filters correctly', () => {
    const { result } = renderHook(() => useFilters({}))
    
    act(() => {
      result.current.updateFilter('name', 'test')
    })
    
    expect(result.current.filters.name).toBe('test')
  })
})
```

## Performance y Optimización

### Code Splitting por Rutas
```tsx
// Lazy loading automático con TanStack Router
const LazyComponent = lazy(() => import('./ExpensiveComponent'))

export const Route = createFileRoute('/expensive')({
  component: () => (
    <Suspense fallback={<div>Loading...</div>}>
      <LazyComponent />
    </Suspense>
  )
})
```

### Memoización de Componentes
```tsx
import { memo, useMemo } from 'react'

const ExpensiveComponent = memo(({ data }: { data: any[] }) => {
  const processedData = useMemo(() => {
    return data.map(item => ({ ...item, processed: true }))
  }, [data])
  
  return <div>{/* Render processedData */}</div>
})
```

### Optimización de Queries
```tsx
const { data } = useQuery({
  queryKey: ['trucks', filters],
  queryFn: () => truckApi.list(filters),
  staleTime: 5 * 60 * 1000, // 5 minutos
  cacheTime: 10 * 60 * 1000, // 10 minutos
  refetchOnWindowFocus: false
})
```

## Variables de Entorno

### Desarrollo (.env.development)
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=HyperLogix Dev
VITE_ENABLE_DEVTOOLS=true
```

### Producción (.env.production)
```env
VITE_API_BASE_URL=https://api.hyperlogix.com/api/v1
VITE_APP_TITLE=HyperLogix
VITE_ENABLE_DEVTOOLS=false
```

### Uso en Código
```tsx
import { env } from '@/env'

const apiUrl = env.VITE_API_BASE_URL
const appTitle = env.VITE_APP_TITLE
```

## Deployment

### Build para Producción
```bash
bun build
```

### Preview de Producción
```bash
bun preview
```

### Archivos Generados
```
dist/
├── index.html
├── assets/
│   ├── index-[hash].js
│   ├── index-[hash].css
│   └── vendor-[hash].js
└── vite.svg
```

## Herramientas de Desarrollo

### Extensiones VS Code Recomendadas
- TypeScript Hero
- Tailwind CSS IntelliSense
- ES7+ React/Redux/React-Native snippets
- Auto Rename Tag
- Bracket Pair Colorizer

### DevTools
- React Developer Tools
- TanStack Query DevTools (incluidas en desarrollo)
- TanStack Router DevTools (incluidas en desarrollo)

## Resolución de Problemas

### Errores Comunes

#### 1. Error de importación de tipos API
```bash
# Regenerar cliente API
bun run generate-api
```

#### 2. Error de Tailwind CSS no aplicándose
```bash
# Verificar configuración en tailwind.config.js
# Asegurar que los paths incluyan todos los archivos
```

#### 3. Problemas con hot reload
```bash
# Limpiar cache de Vite
rm -rf node_modules/.vite
bun start
```

## Mejores Prácticas

### Estructura de Componentes
```tsx
// ✅ Bueno - Componente bien estructurado
interface Props {
  title: string
  onSave: (data: any) => void
}

export function MyComponent({ title, onSave }: Props) {
  const [data, setData] = useState(null)
  
  const handleSubmit = useCallback(() => {
    onSave(data)
  }, [data, onSave])
  
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">{title}</h1>
      {/* Resto del componente */}
    </div>
  )
}
```

### Gestión de Estados
```tsx
// ✅ Bueno - Estado bien tipado
interface UserState {
  user: User | null
  loading: boolean
  error: string | null
}

const [state, setState] = useState<UserState>({
  user: null,
  loading: false,
  error: null
})
```

### Manejo de Errores
```tsx
// ✅ Bueno - Error boundary
function ErrorBoundary({ children }: { children: React.ReactNode }) {
  return (
    <ErrorBoundary
      fallback={<div>Something went wrong</div>}
      onError={(error) => console.error(error)}
    >
      {children}
    </ErrorBoundary>
  )
}
```

## Recursos Adicionales

- [TanStack Router Docs](https://tanstack.com/router)
- [TanStack Query Docs](https://tanstack.com/query)
- [Tailwind CSS Docs](https://tailwindcss.com)
- [Shadcn/ui Docs](https://ui.shadcn.com)
- [Vitest Docs](https://vitest.dev)
