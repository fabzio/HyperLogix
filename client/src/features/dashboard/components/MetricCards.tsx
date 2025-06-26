import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Clock, Package, Route as RouteIcon, Truck } from 'lucide-react'
import { useOperationStore } from '../store/operation'

export function MetricCards() {
  const { metrics, plgNetwork, isConnected } = useOperationStore()

  // Calculate real-time metrics
  const fleetUtilization = metrics?.fleetUtilizationPercentage ?? 0
  const deliveryCompletion = metrics?.completionPercentage ?? 0
  const routeEfficiency = metrics?.deliveryEfficiencyPercentage ?? 0
  const avgDeliveryTime = metrics?.averageDeliveryTimeMinutes ?? 0

  // Fleet status from PLG network
  const totalTrucks = plgNetwork?.trucks?.length ?? 0
  const activeTrucks =
    plgNetwork?.trucks?.filter((truck) => truck.status === 'ACTIVE')?.length ??
    0
  const idleTrucks =
    plgNetwork?.trucks?.filter((truck) => truck.status === 'IDLE')?.length ?? 0

  // Orders status
  const completedOrders =
    plgNetwork?.orders?.filter((order) => order.status === 'COMPLETED')
      ?.length ?? 0
  const pendingOrders =
    plgNetwork?.orders?.filter(
      (order) =>
        order.status === 'PENDING' ||
        order.status === 'CALCULATING' ||
        order.status === 'IN_PROGRESS',
    )?.length ?? 0

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {/* Fleet Utilization Card */}
      <Card className="metric-card border-l-4 border-blue-500 dark:border-blue-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">
            Utilización de Flota
          </CardTitle>
          <div className="flex items-center gap-2">
            <Truck className="h-4 w-4 text-blue-500 dark:text-blue-400" />
            {isConnected ? (
              <Badge
                variant="outline"
                className="bg-green-500/10 text-green-500 border-green-500/20"
              >
                En Vivo
              </Badge>
            ) : (
              <Badge
                variant="outline"
                className="bg-red-500/10 text-red-500 border-red-500/20"
              >
                Desconectado
              </Badge>
            )}
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">
            {fleetUtilization.toFixed(1)}%
          </div>
          <p className="text-xs text-muted-foreground">
            {activeTrucks} activos, {idleTrucks} en espera de {totalTrucks}{' '}
            total
          </p>
          <Progress value={fleetUtilization} className="h-2 mt-3" />
        </CardContent>
      </Card>

      {/* Delivery Performance Card */}
      <Card className="metric-card border-l-4 border-green-500 dark:border-green-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">
            Entregas Completadas
          </CardTitle>
          <Package className="h-4 w-4 text-green-500 dark:text-green-400" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">
            {deliveryCompletion.toFixed(1)}%
          </div>
          <p className="text-xs text-muted-foreground">
            {completedOrders} completadas, {pendingOrders} pendientes
          </p>
          <Progress value={deliveryCompletion} className="h-2 mt-3" />
        </CardContent>
      </Card>

      {/* Route Efficiency Card */}
      <Card className="metric-card border-l-4 border-purple-500 dark:border-purple-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">
            Eficiencia de Entregas
          </CardTitle>
          <RouteIcon className="h-4 w-4 text-purple-500 dark:text-purple-400" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">
            {routeEfficiency.toFixed(1)}%
          </div>
          <p className="text-xs text-muted-foreground">
            GLP entregado vs solicitado
          </p>
          <Progress value={routeEfficiency} className="h-2 mt-3" />
        </CardContent>
      </Card>

      {/* Average Delivery Time Card */}
      <Card className="metric-card border-l-4 border-orange-500 dark:border-orange-600 glassmorphism">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium">Tiempo Promedio</CardTitle>
          <Clock className="h-4 w-4 text-orange-500 dark:text-orange-400" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">
            {avgDeliveryTime.toFixed(0)}min
          </div>
          <p className="text-xs text-muted-foreground">
            Tiempo promedio de entrega
          </p>
          <Progress
            value={Math.min((avgDeliveryTime / 60) * 100, 100)}
            className="h-2 mt-3"
          />
        </CardContent>
      </Card>
    </div>
  )
}
