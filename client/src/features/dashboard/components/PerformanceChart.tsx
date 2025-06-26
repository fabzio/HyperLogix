import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { TrendingUp } from 'lucide-react'
import { useMemo } from 'react'
import {
  Bar,
  BarChart,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useOperationStore } from '../store/operation'

export function PerformanceChart() {
  const { metrics, plgNetwork, isConnected } = useOperationStore()

  // Generate historical data points (simulated for demo)
  const performanceData = useMemo(() => {
    const now = new Date()
    const dataPoints = []

    for (let i = 11; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 5 * 60 * 1000) // 5-minute intervals
      const timeStr = time.toLocaleTimeString('es-ES', {
        hour: '2-digit',
        minute: '2-digit',
      })

      // Simulate some historical variation around current metrics
      const baseFleet = metrics?.fleetUtilizationPercentage ?? 0
      const baseDelivery = metrics?.completionPercentage ?? 0
      const baseEfficiency = metrics?.deliveryEfficiencyPercentage ?? 0

      dataPoints.push({
        time: timeStr,
        fleetUtilization: Math.max(0, baseFleet + (Math.random() - 0.5) * 20),
        deliveryRate: Math.max(0, baseDelivery + (Math.random() - 0.5) * 15),
        efficiency: Math.max(0, baseEfficiency + (Math.random() - 0.5) * 10),
        fuelConsumption:
          (metrics?.averageFuelConsumptionPerKm ?? 0) +
          (Math.random() - 0.5) * 2,
      })
    }

    return dataPoints
  }, [metrics])

  // Fleet status data
  const fleetData = useMemo(() => {
    if (!plgNetwork?.trucks) return []

    const statusCounts = plgNetwork.trucks.reduce(
      (acc, truck) => {
        acc[truck.status] = (acc[truck.status] || 0) + 1
        return acc
      },
      {} as Record<string, number>,
    )

    return Object.entries(statusCounts).map(([status, count]) => ({
      status:
        status === 'ACTIVE'
          ? 'Activos'
          : status === 'IDLE'
            ? 'En Espera'
            : status === 'MAINTENANCE'
              ? 'Mantenimiento'
              : status === 'BROKEN_DOWN'
                ? 'Averiados'
                : status,
      count,
      color:
        status === 'ACTIVE'
          ? '#10b981'
          : status === 'IDLE'
            ? '#f59e0b'
            : status === 'MAINTENANCE'
              ? '#6366f1'
              : status === 'BROKEN_DOWN'
                ? '#ef4444'
                : '#6b7280',
    }))
  }, [plgNetwork])

  // Orders status data
  const ordersData = useMemo(() => {
    if (!plgNetwork?.orders) return []

    const statusCounts = plgNetwork.orders.reduce(
      (acc, order) => {
        acc[order.status] = (acc[order.status] || 0) + 1
        return acc
      },
      {} as Record<string, number>,
    )

    return Object.entries(statusCounts).map(([status, count]) => ({
      status:
        status === 'COMPLETED'
          ? 'Completadas'
          : status === 'IN_PROGRESS'
            ? 'En Progreso'
            : status === 'CALCULATING'
              ? 'Calculando'
              : status === 'PENDING'
                ? 'Pendientes'
                : status,
      count,
      color:
        status === 'COMPLETED'
          ? '#10b981'
          : status === 'IN_PROGRESS'
            ? '#3b82f6'
            : status === 'CALCULATING'
              ? '#f59e0b'
              : status === 'PENDING'
                ? '#6b7280'
                : '#ef4444',
    }))
  }, [plgNetwork])

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            Métricas en Tiempo Real
          </CardTitle>
          {isConnected && (
            <Badge
              variant="outline"
              className="bg-green-500/10 text-green-500 border-green-500/20"
            >
              Actualizando
            </Badge>
          )}
        </div>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="performance" className="w-full">
          <TabsList className="grid w-full max-w-md grid-cols-3">
            <TabsTrigger value="performance">Rendimiento</TabsTrigger>
            <TabsTrigger value="fleet">Flota</TabsTrigger>
            <TabsTrigger value="orders">Pedidos</TabsTrigger>
          </TabsList>

          <TabsContent value="performance" className="mt-4">
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">
                    {(metrics?.fleetUtilizationPercentage ?? 0).toFixed(1)}%
                  </div>
                  <div className="text-sm text-muted-foreground">
                    Utilización Actual
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-600">
                    {(metrics?.deliveryEfficiencyPercentage ?? 0).toFixed(1)}%
                  </div>
                  <div className="text-sm text-muted-foreground">
                    Eficiencia
                  </div>
                </div>
              </div>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={performanceData}>
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="fleetUtilization"
                    stroke="#3b82f6"
                    strokeWidth={2}
                    dot={false}
                    name="Utilización Flota (%)"
                  />
                  <Line
                    type="monotone"
                    dataKey="efficiency"
                    stroke="#10b981"
                    strokeWidth={2}
                    dot={false}
                    name="Eficiencia (%)"
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </TabsContent>

          <TabsContent value="fleet" className="mt-4">
            <div className="space-y-4">
              <div className="text-center">
                <div className="text-2xl font-bold">
                  {plgNetwork?.trucks?.length ?? 0}
                </div>
                <div className="text-sm text-muted-foreground">
                  Total de Camiones
                </div>
              </div>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={fleetData}>
                  <XAxis dataKey="status" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="count" fill="#3b82f6" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </TabsContent>

          <TabsContent value="orders" className="mt-4">
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-600">
                    {plgNetwork?.orders?.filter((o) => o.status === 'COMPLETED')
                      .length ?? 0}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    Completados
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">
                    {plgNetwork?.orders?.filter(
                      (o) => o.status === 'IN_PROGRESS',
                    ).length ?? 0}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    En Progreso
                  </div>
                </div>
              </div>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={ordersData}>
                  <XAxis dataKey="status" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="count" fill="#8b5cf6" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  )
}
