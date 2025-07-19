import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import type { Order } from '@/domain/Order'
import type { Truck } from '@/domain/Truck'
import { cn } from '@/lib/utils'
import { useNavigate, useSearch } from '@tanstack/react-router'
import {
  ArrowLeft,
  Clock,
  Droplets,
  MapPin,
  Package,
  Truck as TruckIcon,
  User,
} from 'lucide-react'
import { useWatchOperation } from '../../../hooks/useOperation'

const statusMap = {
  PENDING: 'bg-yellow-600/10 text-yellow-600 border-yellow-600/30',
  CALCULATING: 'bg-blue-600/10 text-blue-600 border-blue-600/30',
  IN_PROGRESS: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  COMPLETED: 'bg-green-600/10 text-green-600 border-green-600/30',
}

const statusText = {
  PENDING: 'Pendiente',
  CALCULATING: 'Calculando',
  IN_PROGRESS: 'En Progreso',
  COMPLETED: 'Completado',
}

export default function OrderDetail() {
  const { orderId } = useSearch({ from: '/_auth/map' })
  const { plgNetwork, routes, simulationTime } = useWatchOperation()
  const navigate = useNavigate({ from: '/map' })

  const order = plgNetwork?.orders?.find((o: Order) => o.id === orderId)

  // Find trucks assigned to this order (if routes are available)
  const assignedTrucks = routes?.stops
    ? Object.entries(routes.stops)
        .filter(([_, truckStops]) =>
          truckStops.some((stop) => stop.node.id === orderId),
        )
        .map(([truckId, truckStops]) => {
          const truck = plgNetwork?.trucks?.find((t: Truck) => t.id === truckId)
          const orderStop = truckStops.find((stop) => stop.node.id === orderId)
          return {
            truck,
            stop: orderStop,
            truckId,
          }
        })
    : []

  if (!order) {
    return (
      <Card className="w-full">
        <CardContent className="p-6">
          <div className="text-center text-muted-foreground">
            Orden no encontrada
          </div>
        </CardContent>
      </Card>
    )
  }

  // Calculate time information
  const maxDate = new Date(order.maxDeliveryDate)
  const createdDate = new Date(order.date)
  const limitDate = new Date(order.limitTime)
  const currentDate = simulationTime ? new Date(simulationTime) : new Date()

  const totalTime = maxDate.getTime() - createdDate.getTime()
  const elapsedTime = currentDate.getTime() - createdDate.getTime()
  const timeProgress = Math.max(
    0,
    Math.min(100, (elapsedTime / totalTime) * 100),
  )

  const remainingTime = Math.max(
    Math.ceil((maxDate.getTime() - currentDate.getTime()) / (1000 * 60 * 60)),
    0,
  )

  const deliveryProgress = (order.deliveredGLP / order.requestedGLP) * 100

  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() =>
            navigate({
              to: '/map',
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
            className={cn(
              'text-xs',
              statusMap[order.status as keyof typeof statusMap] ||
                'bg-gray-600/10 text-gray-600 border-gray-600/30',
            )}
          >
            {statusText[order.status as keyof typeof statusText] ||
              order.status}
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
              <span className="text-sm font-medium">{order.clientId}</span>
            </div>
          </div>

          <Separator />

          {/* Delivery Progress */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Droplets className="h-4 w-4 text-orange-500" />
              <span className="text-sm font-medium">Progreso de Entrega</span>
            </div>
            <div className="ml-6 space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span>Entregado:</span>
                <span className="font-medium">
                  {order.deliveredGLP.toFixed(1)} /{' '}
                  {order.requestedGLP.toFixed(1)} m³
                </span>
              </div>
              <div className="relative">
                <Progress value={deliveryProgress} className="h-6" />
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-xs font-medium text-white drop-shadow-sm">
                    {Math.round(deliveryProgress)}%
                  </span>
                </div>
              </div>
            </div>
          </div>

          <Separator />

          {/* Time Information */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-purple-500" />
              <span className="text-sm font-medium">Información de Tiempo</span>
            </div>
            <div className="ml-6 space-y-3">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <span className="text-muted-foreground">Creado:</span>
                  <div className="font-medium">
                    {createdDate.toLocaleDateString()}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {createdDate.toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </div>
                </div>
                <div>
                  <span className="text-muted-foreground">Límite:</span>
                  <div className="font-medium">
                    {limitDate.toLocaleDateString()}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {limitDate.toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </div>
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between text-sm mb-2">
                  <span>Tiempo restante:</span>
                  <span
                    className={cn(
                      'font-medium',
                      remainingTime <= 2
                        ? 'text-red-500'
                        : remainingTime <= 6
                          ? 'text-yellow-500'
                          : 'text-green-500',
                    )}
                  >
                    {remainingTime > 0 ? `${remainingTime}h` : 'Vencido'}
                  </span>
                </div>
                <div className="relative">
                  <Progress value={timeProgress} className="h-4" />
                </div>
              </div>
            </div>
          </div>

          <Separator />

          {/* Assigned Trucks Section */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <TruckIcon className="h-4 w-4 text-indigo-500" />
              <span className="text-sm font-medium">
                Camiones Asignados ({assignedTrucks.length})
              </span>
            </div>
            <div className="ml-6">
              {assignedTrucks.length > 0 ? (
                <div className="space-y-2">
                  {assignedTrucks.map(({ truck, stop, truckId }) => (
                    <div
                      key={truckId}
                      className="flex items-center justify-between p-3 rounded-md border bg-card hover:bg-accent/50 transition-colors cursor-pointer"
                      onClick={() =>
                        navigate({
                          to: '/map',
                          search: {
                            truckId: Number(truckId),
                            orderId: undefined,
                          },
                        })
                      }
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          navigate({
                            to: '/map',
                            search: {
                              truckId: Number(truckId),
                              orderId: undefined,
                            },
                          })
                        }
                      }}
                    >
                      <div className="flex items-center gap-3">
                        <TruckIcon className="h-4 w-4 text-indigo-500" />
                        <div className="flex flex-col gap-1">
                          <span className="font-medium text-sm">
                            {truck?.code || `Camión ${truckId}`}
                          </span>
                          {truck?.type && (
                            <Badge
                              variant="secondary"
                              className="w-fit text-xs"
                            >
                              {truck.type}
                            </Badge>
                          )}
                        </div>
                      </div>
                      <div className="flex flex-col items-end gap-1">
                        {stop?.arrivalTime && (
                          <div className="flex items-center gap-1 text-xs text-muted-foreground">
                            <Clock className="h-3 w-3" />
                            {new Date(stop.arrivalTime).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </div>
                        )}
                        <Badge variant="outline" className="text-xs">
                          {truck?.status || 'Desconocido'}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-sm text-muted-foreground italic">
                  No hay camiones asignados actualmente
                </div>
              )}
            </div>
          </div>

          <Separator />

          {/* Location Information */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-green-500" />
              <span className="text-sm font-medium">Ubicación de Entrega</span>
            </div>
            <div className="ml-6">
              <div className="text-sm text-muted-foreground">
                <div>X: {order.location.x.toFixed(6)}</div>
                <div>Y: {order.location.y.toFixed(6)}</div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2 pt-4">
            <Button
              variant="outline"
              size="sm"
              className="flex-1"
              onClick={() => {
                // Navigate back to map
                navigate({
                  to: '/map',
                  search: { orderId: undefined },
                })
              }}
            >
              <MapPin className="h-3 w-3 mr-1" />
              Ver en Mapa
            </Button>
            {assignedTrucks.length > 0 && (
              <Button
                variant="outline"
                size="sm"
                className="flex-1"
                onClick={() => {
                  // Navigate to the first assigned truck
                  navigate({
                    to: '/map',
                    search: {
                      truckId: Number(assignedTrucks[0].truckId),
                      orderId: undefined,
                    },
                  })
                }}
              >
                <TruckIcon className="h-3 w-3 mr-1" />
                Ver Camión
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
