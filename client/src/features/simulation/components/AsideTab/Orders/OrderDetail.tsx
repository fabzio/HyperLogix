import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import { useSimulationStore } from '@/features/simulation/store/simulation'
import { useNavigate, useSearch } from '@tanstack/react-router'
import {
  ArrowLeft,
  Calendar,
  Clock,
  Droplets,
  MapPin,
  Package,
  Truck,
  User,
} from 'lucide-react'

export default function OrderDetail() {
  const { orderId } = useSearch({ from: '/_auth/simulacion' })
  const { plgNetwork, simulationTime, routes } = useSimulationStore()
  const navigate = useNavigate({ from: '/simulacion' })

  const order = plgNetwork?.orders.find((o) => o.id === orderId)
  const stops = routes?.stops || {}

  // Find trucks assigned to this order
  const assignedTrucks = Object.entries(stops)
    .filter(([_, truckStops]) =>
      truckStops.some((stop) => stop.node.id === orderId),
    )
    .map(([truckId, truckStops]) => {
      const truck = plgNetwork?.trucks.find((t) => t.id === truckId)
      const orderStop = truckStops.find((stop) => stop.node.id === orderId)
      return {
        truck,
        stop: orderStop,
        truckId,
      }
    })

  if (!order) {
    return <div>Orden no encontrada</div>
  }

  const maxDate = new Date(order.maxDeliveryDate)
  const createdDate = new Date(order.date)
  const simulationDate = new Date(simulationTime || '')
  const remainingTime = Math.max(
    Math.ceil(
      (maxDate.getTime() - simulationDate.getTime()) / (1000 * 60 * 60),
    ),
    0,
  )

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-600/10 text-yellow-600 border-yellow-600/30'
      case 'CALCULATING':
        return 'bg-blue-600/10 text-blue-600 border-blue-600/30'
      case 'IN_PROGRESS':
        return 'bg-orange-600/10 text-orange-600 border-orange-600/30'
      case 'COMPLETED':
        return 'bg-green-600/10 text-green-600 border-green-600/30'
      default:
        return 'bg-gray-600/10 text-gray-600 border-gray-600/30'
    }
  }

  const getStatusText = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'Pendiente'
      case 'CALCULATING':
        return 'Calculando'
      case 'IN_PROGRESS':
        return 'En Progreso'
      case 'COMPLETED':
        return 'Completado'
      default:
        return status
    }
  }

  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() =>
            navigate({
              to: '/simulacion',
              search: { orderId: undefined },
            })
          }
          className="flex items-center gap-1 text-sm w-fit p-0 h-auto underline hover:no-underline"
        >
          <ArrowLeft className="h-3 w-3" />
          Volver
        </Button>

        <CardTitle className="flex items-center gap-2">
          <Package className="h-5 w-5" />
          Pedido #{order.id.slice(-5)}
          <Badge
            variant="outline"
            className={`text-xs ${getStatusColor(order.status)}`}
          >
            {getStatusText(order.status)}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="space-y-4">
          {/* Client Information */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <User className="h-4 w-4 text-blue-500" />
              <span className="text-sm font-medium">Cliente</span>
            </div>
            <div className="ml-6">
              <span className="text-sm">{order.clientId}</span>
            </div>
          </div>

          <Separator />

          {/* Assigned Trucks Section */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Truck className="h-4 w-4 text-indigo-500" />
              <span className="text-sm font-medium">Camiones Asignados</span>
            </div>
            <div className="ml-6">
              {assignedTrucks.length > 0 ? (
                <div className="space-y-2">
                  {assignedTrucks.map(({ truck, stop, truckId }) => (
                    <div
                      key={truckId}
                      className="flex items-center justify-between p-2 rounded-md border bg-card hover:bg-accent/50 transition-colors cursor-pointer"
                      onClick={() =>
                        navigate({
                          to: '/simulacion',
                          search: { truckId: +truckId, orderId: undefined },
                        })
                      }
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          navigate({
                            to: '/simulacion',
                            search: { truckId: +truckId, orderId: undefined },
                          })
                        }
                      }}
                    >
                      <div className="flex items-center gap-3">
                        <Truck className="h-4 w-4 text-indigo-500" />
                        <div className="flex flex-col gap-1">
                          <span className="font-medium text-sm">
                            {truck?.code || `Camión ${truckId}`}
                          </span>
                          {truck?.type && (
                            <Badge variant="outline" className="w-fit text-xs">
                              {truck.type}
                            </Badge>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-1 text-xs text-muted-foreground">
                        <Clock className="h-3 w-3" />
                        {stop?.arrivalTime
                          ? new Date(stop.arrivalTime).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit',
                            })
                          : '--:--'}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-sm text-muted-foreground">
                  No hay camiones asignados
                </div>
              )}
            </div>
          </div>

          <Separator />

          {/* GLP Information */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Droplets className="h-4 w-4 text-orange-500" />
              <span className="text-sm font-medium">GLP</span>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">
                  Entregado / Solicitado
                </span>
                <span className="text-xs text-muted-foreground">
                  {order.deliveredGLP}m³ / {order.requestedGLP}m³
                </span>
              </div>
              <div className="relative">
                <Progress
                  value={(order.deliveredGLP / order.requestedGLP) * 100}
                  className="h-6"
                />
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-xs font-medium text-white drop-shadow-sm">
                    {Math.round(
                      (order.deliveredGLP / order.requestedGLP) * 100,
                    )}
                    %
                  </span>
                </div>
              </div>
            </div>
          </div>

          <Separator />

          {/* Location Information */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-green-500" />
              <span className="text-sm font-medium">Ubicación</span>
            </div>
            <div className="ml-6 text-sm text-muted-foreground">
              X: {order.location.x.toFixed(2)}, Y: {order.location.y.toFixed(2)}
            </div>
          </div>

          <Separator />

          {/* Timing Information */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-purple-500" />
              <span className="text-sm font-medium">Tiempos</span>
            </div>
            <div className="ml-6 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">Creado:</span>
                <span className="text-xs">
                  {createdDate.toLocaleDateString()}{' '}
                  {createdDate.toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">
                  Entrega máxima:
                </span>
                <span className="text-xs">
                  {maxDate.toLocaleDateString()}{' '}
                  {maxDate.toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">
                  Tiempo restante:
                </span>
                <Badge
                  variant="outline"
                  className={`text-xs ${
                    order.status === 'COMPLETED'
                      ? 'bg-green-600/10 text-green-600 border-green-600/30'
                      : remainingTime > 0
                        ? 'bg-blue-600/10 text-blue-600 border-blue-600/30'
                        : 'bg-red-600/10 text-red-600 border-red-600/30'
                  }`}
                >
                  {order.status === 'COMPLETED'
                    ? 'A tiempo'
                    : remainingTime > 0
                      ? `${remainingTime} horas`
                      : 'Retrasado'}
                </Badge>
              </div>
            </div>
          </div>

          <Separator />

          {/* Priority */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-red-500" />
              <span className="text-sm font-medium">Prioridad</span>
            </div>
            <div className="ml-6">
              <Badge variant="outline" className="text-xs">
                {order.status}
              </Badge>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
