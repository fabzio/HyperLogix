import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import type { PLGNetwork } from '@/domain/PLGNetwork'
import type { Routes } from '@/domain/Routes'
import { TruckState } from '@/domain/TruckState'
import { MapPin, Maximize2 } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'

interface MapModalProps {
  network?: PLGNetwork
  routes?: Routes
  simulationTime?: string
  asQuickAction?: boolean
}

export function MapModal({
  network,
  routes,
  simulationTime,
  asQuickAction = false,
}: MapModalProps) {
  const [selectedTruckId, setSelectedTruckId] = useState<string | null>(null)
  const [hoveredPolylineId, setHoveredPolylineId] = useState<string | null>(
    null,
  )

  const getTruckColorById = useCallback((truckId: string) => {
    const colors = [
      '#FF6B6B', // Red
      '#4ECDC4', // Teal
      '#45B7D1', // Blue
      '#96CEB4', // Green
      '#FECA57', // Yellow
      '#FF9FF3', // Pink
      '#54A0FF', // Light Blue
      '#5F27CD', // Purple
    ]
    const hash = truckId
      .split('')
      .reduce((acc, char) => acc + char.charCodeAt(0), 0)
    return colors[hash % colors.length]
  }, [])

  const polylines: MapPolyline[] = useMemo(() => {
    if (!routes?.paths) return []

    const pathPolylines: MapPolyline[] = Object.entries(routes.paths).map(
      ([truckId, paths]) => ({
        id: truckId,
        points: paths.flatMap((path) =>
          (path.points || []).map(
            (point) => [point.x, point.y] as [number, number],
          ),
        ),
        stroke: getTruckColorById(truckId),
        strokeWidth: selectedTruckId === truckId ? 3 : 2,
        type: 'path' as const,
      }),
    )

    const roadblockPolylines: MapPolyline[] =
      network?.roadblocks?.map((roadblock, index) => ({
        id: `roadblock-${index}`,
        points: roadblock.blockedNodes.map(
          (point) => [point.x, point.y] as [number, number],
        ),
        stroke: '#DC2626',
        strokeWidth: 3,
        type: 'roadblock' as const,
        startTime: roadblock.start,
        endTime: roadblock.end,
      })) || []

    return [...pathPolylines, ...roadblockPolylines]
  }, [routes?.paths, network?.roadblocks, selectedTruckId, getTruckColorById])

  const handlePolylineHover = (truckId: string | null) => {
    setHoveredPolylineId(truckId)
  }

  const handlePolylineClick = (truckId: string) => {
    setSelectedTruckId(selectedTruckId === truckId ? null : truckId)
  }

  if (!network) {
    return (
      <Dialog>
        <DialogTrigger asChild>
          {asQuickAction ? (
            <Button
              variant="outline"
              disabled
              className="flex flex-col h-20 items-center justify-center gap-2"
            >
              <MapPin className="h-5 w-5 text-gray-400" />
              <span className="text-xs">Ver Mapa</span>
            </Button>
          ) : (
            <Button variant="outline" disabled>
              <MapPin className="h-4 w-4 mr-2" />
              Ver Mapa
            </Button>
          )}
        </DialogTrigger>
      </Dialog>
    )
  }

  return (
    <Dialog>
      <DialogTrigger asChild>
        {asQuickAction ? (
          <Button
            variant="outline"
            className="flex flex-col h-20 items-center justify-center gap-2"
          >
            <MapPin className="h-5 w-5 text-cyan-500" />
            <span className="text-xs">Ver Mapa</span>
          </Button>
        ) : (
          <Button variant="outline">
            <MapPin className="h-4 w-4 mr-2" />
            Ver Mapa
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-6xl max-h-[90vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Maximize2 className="h-5 w-5" />
            Mapa en Tiempo Real
            {simulationTime && (
              <span className="text-sm font-normal text-muted-foreground ml-2">
                {new Date(simulationTime).toLocaleString()}
              </span>
            )}
          </DialogTitle>
        </DialogHeader>
        <div className="w-full flex justify-center">
          <div
            className="w-full max-w-4xl"
            style={{
              aspectRatio: '70/50', // Maintain map grid proportions
              maxHeight: '70vh',
            }}
          >
            <DynamicMap
              trucks={
                network.trucks.filter(
                  (truck) => truck.status !== TruckState.IDLE,
                ) || []
              }
              stations={network.stations || []}
              orders={
                network.orders.filter(
                  (order) =>
                    order.status !== 'COMPLETED' &&
                    (!simulationTime || order.date <= simulationTime),
                ) || []
              }
              polylines={polylines.filter(
                (line) =>
                  line.type !== 'roadblock' ||
                  !simulationTime ||
                  !line.startTime ||
                  !line.endTime ||
                  (simulationTime >= line.startTime &&
                    simulationTime <= line.endTime),
              )}
              onPolylineHover={handlePolylineHover}
              onPolylineClick={handlePolylineClick}
              hoveredPolylineId={hoveredPolylineId}
            />
          </div>
        </div>
        {selectedTruckId && (
          <div className="mt-4 p-3 bg-muted rounded-lg">
            <h4 className="font-medium mb-2">Camión {selectedTruckId}</h4>
            <div className="text-sm text-muted-foreground">
              {(() => {
                const truck = network.trucks.find(
                  (t) => t.id === selectedTruckId,
                )
                if (!truck) return 'Información no disponible'

                const stops = routes?.stops?.[selectedTruckId] || []
                const deliveryStops = stops.filter(
                  (stop) => stop.node.type === 'DELIVERY',
                )

                return (
                  <div className="space-y-1">
                    <div>Estado: {truck.status}</div>
                    <div>
                      Capacidad: {truck.currentCapacity}/{truck.maxCapacity} L
                    </div>
                    <div>
                      Combustible: {truck.currentFuel.toFixed(1)}/
                      {truck.fuelCapacity} gal
                    </div>
                    <div>Entregas asignadas: {deliveryStops.length}</div>
                    {truck.location && (
                      <div>
                        Ubicación: ({truck.location.x.toFixed(1)},{' '}
                        {truck.location.y.toFixed(1)})
                      </div>
                    )}
                  </div>
                )
              })()}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
