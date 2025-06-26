import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { NodeType } from '@/domain/NodeType'
import { useWatchOperation } from '@/features/dashboard/hooks/useOperation'
import { cn } from '@/lib/utils'
import { useSessionStore } from '@/store/session'
import { Progress } from '@radix-ui/react-progress'
import { createFileRoute } from '@tanstack/react-router'
import { Clock, Droplets, Fuel, MapPin, TruckIcon } from 'lucide-react'

export const Route = createFileRoute('/_auth/driver')({
  component: RouteComponent,
})

function RouteComponent() {
  const { username } = useSessionStore()
  const { plgNetwork, routes } = useWatchOperation()
  if (!username || !plgNetwork) {
    return <div>Loading...</div>
  }
  const { trucks } = plgNetwork
  const truck = trucks.find((t) => t.code === username)
  if (!truck) {
    return <div>Camión inactivo o no encontrado</div>
  }
  const filteredStops = routes?.stops[truck?.id] || []
  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2">
          <TruckIcon className="h-5 w-5" />
          {truck.code}
          {truck.type && (
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

const typeMap = {
  TA: 'bg-blue-600/10 text-blue-500 border-blue-600/30',
  TB: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  TC: 'bg-purple-600/10 text-purple-600 border-purple-600/30',
  TD: 'bg-pink-600/10 text-pink-600 border-pink-600/30',
}
