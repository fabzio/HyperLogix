import Typography from '@/components/typography'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import {
  Activity,
  BarChart3,
  Clock,
  Fuel,
  Package,
  TrendingUp,
  Truck,
  Zap,
} from 'lucide-react'
import { useWatchOperation } from '../../../hooks/useOperation'

export default function Metrics() {
  const { metrics, plgNetwork } = useWatchOperation()

  if (!metrics || !plgNetwork) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center">
          <Activity className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
          <p className="text-sm text-muted-foreground">
            Esperando métricas de operación...
          </p>
        </div>
      </div>
    )
  }

  const metricsCards = [
    {
      title: 'Utilización de Flota',
      value: `${(metrics.fleetUtilizationPercentage || 0).toFixed(1)}%`,
      icon: Truck,
      description: 'Camiones activos vs. totales',
      progress: metrics.fleetUtilizationPercentage || 0,
      color: 'text-blue-600',
    },
    {
      title: 'Eficiencia de Entrega',
      value: `${(metrics.deliveryEfficiencyPercentage || 0).toFixed(1)}%`,
      icon: Package,
      description: 'Entregas completadas a tiempo',
      progress: metrics.deliveryEfficiencyPercentage || 0,
      color: 'text-green-600',
    },
    {
      title: 'Órdenes Procesadas',
      value: (metrics.totalOrdersProcessed || 0).toString(),
      icon: BarChart3,
      description: `${metrics.pendingOrdersCount || 0} pendientes`,
      color: 'text-purple-600',
    },
    {
      title: 'Consumo de Combustible',
      value: `${(metrics.averageFuelConsumptionPerKm || 0).toFixed(2)} L/km`,
      icon: Fuel,
      description: 'Promedio por kilómetro',
      color: 'text-orange-600',
    },
    {
      title: 'Tiempo de Entrega',
      value: `${(metrics.averageDeliveryTimeMinutes || 0).toFixed(0)} min`,
      icon: Clock,
      description: 'Promedio de entrega',
      color: 'text-cyan-600',
    },
    {
      title: 'Utilización de Capacidad',
      value: `${(metrics.averageCapacityUtilization || 0).toFixed(1)}%`,
      icon: TrendingUp,
      description: 'Capacidad promedio utilizada',
      progress: metrics.averageCapacityUtilization || 0,
      color: 'text-indigo-600',
    },
    {
      title: 'Distancia Total',
      value: `${(metrics.totalDistanceTraveled || 0).toFixed(1)} km`,
      icon: Activity,
      description: 'Recorrido acumulado',
      color: 'text-red-600',
    },
    {
      title: 'Tiempo de Planificación',
      value: `${(metrics.averagePlanificationTimeSeconds || 0).toFixed(1)}s`,
      icon: Zap,
      description: 'Promedio de cálculo de rutas',
      color: 'text-yellow-600',
    },
  ]

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b">
        <Typography variant="h3">Métricas de Operación</Typography>
        <p className="text-sm text-muted-foreground">
          Indicadores clave de rendimiento en tiempo real
        </p>
      </div>

      <div className="flex-1 overflow-auto p-4">
        <div className="grid gap-4">
          {metricsCards.map((metric) => (
            <Card key={metric.title} className="relative overflow-hidden">
              <CardHeader className="pb-2">
                <CardTitle className="flex items-center gap-2 text-sm">
                  <metric.icon className={`h-4 w-4 ${metric.color}`} />
                  {metric.title}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="text-2xl font-bold">{metric.value}</div>
                  <p className="text-xs text-muted-foreground">
                    {metric.description}
                  </p>
                  {metric.progress !== undefined && (
                    <Progress value={metric.progress} className="h-1.5" />
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Resumen adicional */}
        <Card className="mt-4">
          <CardHeader>
            <CardTitle className="text-sm">Resumen de Operación</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <div className="text-muted-foreground">Camiones activos</div>
                <div className="font-medium">
                  {plgNetwork.trucks.filter((t) => t.status !== 'IDLE').length}{' '}
                  / {plgNetwork.trucks.length}
                </div>
              </div>
              <div>
                <div className="text-muted-foreground">
                  Estaciones disponibles
                </div>
                <div className="font-medium">{plgNetwork.stations.length}</div>
              </div>
              <div>
                <div className="text-muted-foreground">Órdenes activas</div>
                <div className="font-medium">
                  {
                    plgNetwork.orders.filter((o) => o.status !== 'COMPLETED')
                      .length
                  }
                </div>
              </div>
              <div>
                <div className="text-muted-foreground">Bloqueos activos</div>
                <div className="font-medium">
                  {plgNetwork.roadblocks?.length || 0}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
