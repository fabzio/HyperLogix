// Mock data for performance charts
export const performanceData = Array(24)
  .fill(0)
  .map((_, i) => ({
    time: `${i}:00`,
    vehiculos: Math.floor(Math.random() * 40) + 20,
    entregas: Math.floor(Math.random() * 30) + 40,
    rutas: Math.floor(Math.random() * 25) + 60,
  }))

// Metrics data
export const metricsData = {
  flota: {
    value: 47,
    details: '38 Vehículos | 12 Rutas',
  },
  entregas: {
    value: 61,
    details: '165 Completadas / 240 Total',
  },
  rutas: {
    value: 90,
    details: 'Ahorro 42km | 120min',
  },
}

// System status data
export const systemStatusData = [
  { name: 'GPS Activo', status: 'Activo', statusType: 'success' },
  { name: 'Monitoreo de Rutas', status: 'Activo', statusType: 'success' },
  { name: 'Servicio de Combustible', status: 'Alerta', statusType: 'warning' },
]

// Alerts data
export const alertsData = [
  { message: '3 vehículos con bajo combustible', type: 'warning' },
  { message: 'Congestión en Ruta A-45', type: 'error' },
  { message: 'Mantenimiento programado: 2 vehículos', type: 'warning' },
]

// Resource allocation data
export const resourcesData = [
  { name: 'Capacidad de Carga', value: 42, color: 'blue' },
  { name: 'Asignación de Vehículos', value: 68, color: 'purple' },
  { name: 'Cobertura de Rutas', value: 35, color: 'cyan' },
]

// Fuel status data
export const fuelData = [
  { name: 'Nivel Promedio', value: 62 },
  { name: 'Consumo Diario', value: 45, details: '1,240L' },
]
