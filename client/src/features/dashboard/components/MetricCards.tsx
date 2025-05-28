import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Map as MapIcon, Route as RouteIcon, Truck } from 'lucide-react'
import { metricsData } from '../data/mock-data'

export function MetricCards() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      <Card className="metric-card border-l-4 border-blue-500 dark:border-blue-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">Flota Activa</CardTitle>
          <Truck className="h-4 w-4 text-blue-500 dark:text-blue-400" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{metricsData.flota.value}%</div>
          <p className="text-xs text-muted-foreground">
            {metricsData.flota.details}
          </p>
          <Progress value={metricsData.flota.value} className="h-2 mt-3" />
        </CardContent>
      </Card>

      <Card className="metric-card border-l-4 border-purple-500 dark:border-purple-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">Entregas</CardTitle>
          <MapIcon className="h-4 w-4 text-purple-500 dark:text-purple-400" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">
            {metricsData.entregas.value}%
          </div>
          <p className="text-xs text-muted-foreground">
            {metricsData.entregas.details}
          </p>
          <Progress value={metricsData.entregas.value} className="h-2 mt-3" />
        </CardContent>
      </Card>

      <Card className="metric-card border-l-4 border-cyan-500 dark:border-cyan-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">
            Eficiencia de Rutas
          </CardTitle>
          <RouteIcon className="h-4 w-4 text-cyan-500 dark:text-cyan-400" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{metricsData.rutas.value}%</div>
          <p className="text-xs text-muted-foreground">
            {metricsData.rutas.details}
          </p>
          <Progress value={metricsData.rutas.value} className="h-2 mt-3" />
        </CardContent>
      </Card>
    </div>
  )
}
