import Typography from '@/components/typography'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import {
  Activity,
  BarChart3,
  Clock,
  Fuel,
  Route,
  Target,
  Timer,
  TrendingUp,
  Truck,
} from 'lucide-react'
import { useSimulationStore } from '../../store/simulation'

export default function Metrics() {
  const { metrics } = useSimulationStore()

  if (!metrics) {
    return (
      <article className="h-full flex flex-col">
        <Typography variant="h3" className="mb-4">
          Métricas de Simulación
        </Typography>
        <Card className="flex-1">
          <CardContent className="pt-6 h-full flex items-center justify-center">
            <p className="text-center text-muted-foreground">
              No hay métricas disponibles.
            </p>
          </CardContent>
        </Card>
      </article>
    )
  }

  const metricsData = [
    {
      title: 'Utilización de la flota',
      value: `${metrics.fleetUtilizationPercentage.toFixed(2)}%`,
      icon: Truck,
      color: 'text-blue-500',
      progress: metrics.fleetUtilizationPercentage,
    },
    {
      title: 'Porcentaje de finalización',
      value: `${metrics.completionPercentage.toFixed(2)}%`,
      icon: Target,
      color: 'text-green-500',
      progress: metrics.completionPercentage,
    },
    {
      title: 'Utilización promedio de capacidad',
      value: `${metrics.averageCapacityUtilization.toFixed(2)}%`,
      icon: BarChart3,
      color: 'text-purple-500',
      progress: metrics.averageCapacityUtilization,
    },
    {
      title: 'Eficiencia de entrega',
      value: `${metrics.deliveryEfficiencyPercentage.toFixed(2)}%`,
      icon: TrendingUp,
      color: 'text-orange-500',
      progress: metrics.deliveryEfficiencyPercentage,
    },
  ]

  const timeMetrics = [
    {
      title: 'Tiempo promedio de entrega',
      value: `${metrics.averageDeliveryTimeMinutes.toFixed(0)} min`,
      icon: Clock,
      color: 'text-indigo-500',
    },
    {
      title: 'Tiempo promedio de planificación',
      value: `${metrics.averagePlanificationTimeSeconds.toFixed(2)} seg`,
      icon: Timer,
      color: 'text-pink-500',
    },
  ]

  const distanceMetrics = [
    {
      title: 'Total de distancia recorrida',
      value: `${metrics.totalDistanceTraveled.toFixed(2)} km`,
      icon: Route,
      color: 'text-cyan-500',
    },
    {
      title: 'Consumo promedio de combustible',
      value: `${metrics.averageFuelConsumptionPerKm.toFixed(2)} L/km`,
      icon: Fuel,
      color: 'text-red-500',
    },
  ]

  return (
    <article className="h-full flex flex-col">
      <Typography variant="h3" className="mb-4">
        Métricas de Simulación
      </Typography>

      <div className="flex-1 overflow-y-auto space-y-4 pr-2">
        {/* Performance Metrics with Progress Bars */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {metricsData.map((metric) => {
            const Icon = metric.icon
            return (
              <Card key={metric.title} className="h-fit">
                <CardHeader className="pb-2">
                  <CardTitle className="flex items-center gap-2 text-xs font-medium">
                    <Icon className={`h-3 w-3 ${metric.color}`} />
                    {metric.title}
                  </CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="space-y-2">
                    <div className="text-lg font-bold">{metric.value}</div>
                    <Progress value={metric.progress} className="h-1.5" />
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>

        <Separator />

        {/* Time Metrics */}
        <div className="space-y-3">
          <Typography
            variant="h4"
            className="text-xs font-medium text-muted-foreground"
          >
            Tiempos
          </Typography>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {timeMetrics.map((metric) => {
              const Icon = metric.icon
              return (
                <Card key={metric.title} className="h-fit">
                  <CardContent className="pt-4 pb-4">
                    <div className="flex items-center gap-2">
                      <div
                        className={`p-1.5 rounded-md ${metric.color} bg-opacity-10`}
                      >
                        <Icon className={`h-3 w-3 ${metric.color}`} />
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-xs font-medium leading-none">
                          {metric.title}
                        </p>
                        <p className="text-sm font-bold">{metric.value}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>

        <Separator />

        {/* Distance and Fuel Metrics */}
        <div className="space-y-3">
          <Typography
            variant="h4"
            className="text-xs font-medium text-muted-foreground"
          >
            Distancia y Combustible
          </Typography>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {distanceMetrics.map((metric) => {
              const Icon = metric.icon
              return (
                <Card key={metric.title} className="h-fit">
                  <CardContent className="pt-4 pb-4">
                    <div className="flex items-center gap-2">
                      <div
                        className={`p-1.5 rounded-md ${metric.color} bg-opacity-10`}
                      >
                        <Icon className={`h-3 w-3 ${metric.color}`} />
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-xs font-medium leading-none">
                          {metric.title}
                        </p>
                        <p className="text-sm font-bold">{metric.value}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>

        {/* Summary Card */}
        <Card className="border-dashed h-fit">
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-sm">
              <Activity className="h-4 w-4 text-emerald-500" />
              Resumen General
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0">
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="space-y-1">
                <p className="text-muted-foreground">Eficiencia Global</p>
                <p className="font-semibold text-sm">
                  {(
                    (metrics.fleetUtilizationPercentage +
                      metrics.deliveryEfficiencyPercentage) /
                    2
                  ).toFixed(1)}
                  %
                </p>
              </div>
              <div className="space-y-1">
                <p className="text-muted-foreground">Estado del Sistema</p>
                <p className="font-semibold text-sm text-green-600">
                  {metrics.completionPercentage > 80
                    ? 'Óptimo'
                    : metrics.completionPercentage > 60
                      ? 'Bueno'
                      : 'Mejorable'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </article>
  )
}
