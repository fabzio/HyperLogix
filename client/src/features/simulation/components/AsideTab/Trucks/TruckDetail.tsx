import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import { NodeType } from '@/domain/NodeType'
import type { Truck } from '@/domain/Truck'
import { useSimulationStore } from '@/features/simulation/store/simulation'
import { cn } from '@/lib/utils'
import { useNavigate } from '@tanstack/react-router'
import {
  ArrowLeft,
  Clock,
  Droplets,
  Fuel,
  MapPin,
  Truck as TruckIcon,
} from 'lucide-react'

interface Props {
  truckId: string
}

const typeMap = {
  TA: 'bg-blue-600/10 text-blue-500 border-blue-600/30',
  TB: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  TC: 'bg-purple-600/10 text-purple-600 border-purple-600/30',
  TD: 'bg-pink-600/10 text-pink-600 border-pink-600/30',
}

export default function TruckDetail({ truckId }: Props) {
  const { routes, plgNetwork } = useSimulationStore()
  const navigate = useNavigate({ from: '/simulacion' })
  const stops = routes?.stops[truckId] || []
  let truck: Truck | undefined
  const filteredStops = stops.filter(
    (stop) => stop.node.type !== NodeType.LOCATION,
  )
  if (plgNetwork) {
    truck = plgNetwork.trucks.find((t) => t.id === truckId)
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
              search: { truckId: undefined },
            })
          }
          className="flex items-center gap-1 text-sm w-fit p-0 h-auto underline hover:no-underline"
        >
          <ArrowLeft className="h-3 w-3" />
          Volver
        </Button>

        <CardTitle className="flex items-center gap-2">
          <TruckIcon className="h-5 w-5" />
          {truck?.code || `Truck ${truckId}`}
          {truck?.type && (
            <Badge
              variant="outline"
              className={cn(
                'text-xs',
                typeMap[truck.type as keyof typeof typeMap],
              )}
            >
              {truck.type}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="space-y-4">
          {/* Truck Details Section */}
          {truck && (
            <div className="space-y-3">
              {/* Fuel Level */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Fuel className="h-4 w-4 text-blue-500" />
                    <span className="text-sm font-medium">Combustible</span>
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {truck.currentFuel.toFixed(1)}gal / {truck.fuelCapacity}gal
                  </span>
                </div>
                <div className="flex gap-1">
                  {Array.from({ length: 10 }, (_, i) => {
                    const threshold = (truck.fuelCapacity / 10) * (i + 1)
                    const isFilled = truck.currentFuel >= threshold
                    return (
                      <div
                        key={`fuel-bar-${truck.id}-${i}`}
                        className={`h-3 flex-1 rounded-sm border ${
                          isFilled
                            ? truck.currentFuel / truck.fuelCapacity > 0.3
                              ? 'bg-green-500 border-green-600'
                              : 'bg-yellow-500 border-yellow-600'
                            : 'bg-gray-200 border-gray-300'
                        }`}
                      />
                    )
                  })}
                </div>
              </div>

              {/* Capacity Tank */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Droplets className="h-4 w-4 text-orange-500" />
                    <span className="text-sm font-medium">Capacidad</span>
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {truck.currentCapacity.toFixed(1)}m³ / {truck.maxCapacity}m³
                  </span>
                </div>
                <div className="relative">
                  <Progress
                    value={(truck.currentCapacity / truck.maxCapacity) * 100}
                    className="h-8"
                  />
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-xs font-medium text-white drop-shadow-sm">
                      {Math.round(
                        (truck.currentCapacity / truck.maxCapacity) * 100,
                      )}
                      %
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">Estado:</span>
                <Badge variant="outline" className="text-xs">
                  {truck.status}
                </Badge>
              </div>
            </div>
          )}

          <Separator />

          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <MapPin className="h-4 w-4" />
            <span>{filteredStops.length} paradas</span>
          </div>

          <div className="space-y-2">
            {filteredStops.map((stop, index) => {
              let displayName = stop.node.id
              let displayType: string = stop.node.type

              if (stop.node.type === NodeType.STATION) {
                const station = plgNetwork?.stations.find(
                  (s) => s.id === stop.node.id,
                )
                displayName = station?.name || stop.node.id
                displayType = 'Estación de recarga'
              } else if (stop.node.type === NodeType.DELIVERY) {
                const order = plgNetwork?.orders.find(
                  (o) => o.id === stop.node.id,
                )
                displayName = order?.clientId || stop.node.id
                displayType = 'Entrega'
              }

              return (
                <div
                  key={stop.node.id}
                  className="flex items-center justify-between p-2 rounded-md border bg-card hover:bg-accent/50 transition-colors cursor-pointer"
                  onClick={() => {
                    if (stop.node.type === NodeType.DELIVERY) {
                      navigate({
                        to: '/simulacion',
                        search: { orderId: stop.node.id },
                      })
                    }
                  }}
                  onKeyDown={(e) => {
                    if (
                      (e.key === 'Enter' || e.key === ' ') &&
                      stop.node.type === NodeType.DELIVERY
                    ) {
                      e.preventDefault()
                      navigate({
                        to: '/simulacion',
                        search: { orderId: stop.node.id },
                      })
                    }
                  }}
                >
                  <div className="flex items-center gap-3">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/10 text-xs font-medium">
                      {index + 1}
                    </span>
                    <div className="flex flex-col gap-1">
                      <span className="font-medium text-sm">{displayName}</span>
                      <Badge
                        variant={
                          stop.node.type === NodeType.DELIVERY
                            ? 'default'
                            : 'secondary'
                        }
                        className="w-fit text-xs"
                      >
                        {displayType}
                      </Badge>
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
              )
            })}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
