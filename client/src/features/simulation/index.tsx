import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { TruckSelectionBanner } from '@/components/TruckSelectionBanner'
import { TruckState } from '@/domain/TruckState'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useCallback, useEffect, useMemo, useState } from 'react'
import AsideTab from './components/AsideTab'
import SimulationEndDialog from './components/SimulationEndDialog'
import SimulationHeader from './components/SimulationHeader'
import {
  useSimulationEndDialog,
  useSimulationWebSocket,
  useStatusSimulation,
  useWatchSimulation,
} from './hooks/useSimulation'
const truckTypeColors: Record<string, string> = {
  TA: '#a855f7', // purple-500
  TB: '#38bdf8', // sky-400
  TC: '#4ade80', // green-400
  TD: '#facc15', // yellow-400
}

export default function Simulation() {
  const [polylineHover, setPolylineHover] = useState<string | null>(null)

  const { truckId } = useSearch({ from: '/_auth/simulacion' })
  const navigate = useNavigate({ from: '/simulacion' })

  // Centralizar la suscripción WebSocket aquí
  useSimulationWebSocket()
  const {
    plgNetwork: network,
    simulationTime,
    routes,
    truckProgress,
  } = useWatchSimulation()
  const { data: status } = useStatusSimulation()

  // Get selected truck info
  const selectedTruck = truckId
    ? network?.trucks.find((t) => t.id === truckId.toString())
    : null

  const getTruckColorById = useCallback(
    (id: string): string => {
      const truck = network?.trucks.find((t) => t.id === id)
      return truckTypeColors[truck?.type ?? 'TA']
    },
    [network?.trucks],
  )
  const { isOpen, endReason, closeDialog } = useSimulationEndDialog(network)
  useEffect(() => {
    if (truckId) {
      setPolylineHover(`${truckId}-path-0`)
    } else {
      setPolylineHover(null)
    }
  }, [truckId])
  const poliLines: MapPolyline[] = useMemo(() => {
    const pathPolylines = routes?.paths
      ? Object.entries(routes.paths).flatMap(([truckId, paths]) => {
          const truckLocation = network?.trucks.find(
            (t) => t.id === truckId,
          )?.location
          const progress = truckProgress?.[truckId] ?? 1

          const fullPath: [number, number][][] = paths.map(
            (path) =>
              path.points?.map((p) => [p.x, p.y] as [number, number]) || [],
          ) ?? [[]]

          // Remover el primer punto de cada segmento
          for (let i = 0; i < fullPath.length; i++) {
            fullPath[i].shift()
          }

          const formattedPath = fullPath.flat()

          // Función para calcular distancia entre dos puntos
          const getDistance = (p1: [number, number], p2: [number, number]) => {
            return Math.sqrt(
              Math.pow(p2[0] - p1[0], 2) + Math.pow(p2[1] - p1[1], 2),
            )
          }

          // Función para interpolar entre dos puntos
          const interpolatePoint = (
            p1: [number, number],
            p2: [number, number],
            ratio: number,
          ): [number, number] => {
            return [
              p1[0] + (p2[0] - p1[0]) * ratio,
              p1[1] + (p2[1] - p1[1]) * ratio,
            ]
          }

          let currentPosition: [number, number] = truckLocation
            ? [truckLocation.x, truckLocation.y]
            : [0, 0]
          let remainingPoints: [number, number][] = [...formattedPath]

          if (formattedPath.length > 0 && progress > 0 && progress < 1) {
            // Calcular distancias acumuladas de cada segmento
            const segmentDistances: number[] = []
            let totalDistance = 0

            for (let i = 0; i < formattedPath.length - 1; i++) {
              const segmentDistance = getDistance(
                formattedPath[i],
                formattedPath[i + 1],
              )
              segmentDistances.push(segmentDistance)
              totalDistance += segmentDistance
            }

            // Encontrar la posición exacta basada en el progreso
            const targetDistance = progress * totalDistance
            let accumulatedDistance = 0
            let currentSegmentIndex = -1
            let remainingDistanceInSegment = 0

            // Buscar en qué segmento está el camión
            for (let i = 0; i < segmentDistances.length; i++) {
              if (accumulatedDistance + segmentDistances[i] >= targetDistance) {
                currentSegmentIndex = i
                remainingDistanceInSegment =
                  targetDistance - accumulatedDistance
                break
              }
              accumulatedDistance += segmentDistances[i]
            }

            if (
              currentSegmentIndex >= 0 &&
              currentSegmentIndex < formattedPath.length - 1
            ) {
              // Calcular la posición exacta dentro del segmento
              const segmentProgress =
                remainingDistanceInSegment /
                segmentDistances[currentSegmentIndex]

              // Interpolar la posición actual
              currentPosition = interpolatePoint(
                formattedPath[currentSegmentIndex],
                formattedPath[currentSegmentIndex + 1],
                segmentProgress,
              )

              // Los puntos restantes incluyen el próximo vértice y todos los siguientes
              remainingPoints = formattedPath.slice(currentSegmentIndex + 1)
            } else if (progress >= 1) {
              // El camión llegó al final
              currentPosition = formattedPath[formattedPath.length - 1]
              remainingPoints = []
            }
          } else if (progress >= 1) {
            // Progreso completo
            currentPosition =
              formattedPath[formattedPath.length - 1] || currentPosition
            remainingPoints = []
          }

          // En caso de cruces, usar la posición del truck como referencia
          // Si el truck está significativamente desviado del path calculado, usar su posición real
          if (truckLocation && formattedPath.length > 0) {
            const distanceToCalculatedPosition = getDistance(
              [truckLocation.x, truckLocation.y],
              currentPosition,
            )

            // Si la distancia es mayor a un umbral, usar la posición real del truck
            const DEVIATION_THRESHOLD = 3.5 // Ajusta según tu escala
            if (distanceToCalculatedPosition > DEVIATION_THRESHOLD) {
              currentPosition = [truckLocation.x, truckLocation.y]

              // Recalcular remaining points desde la posición real
              // Encontrar el punto más cercano en el path
              let closestPointIndex = 0
              let minDistance = Number.POSITIVE_INFINITY

              for (let i = 0; i < formattedPath.length; i++) {
                const distance = getDistance(
                  [truckLocation.x, truckLocation.y],
                  formattedPath[i],
                )
                if (distance < minDistance) {
                  minDistance = distance
                  closestPointIndex = i
                }
              }

              // Usar los puntos restantes desde el punto más cercano
              remainingPoints = formattedPath.slice(closestPointIndex)
            }
          }

          return [
            {
              id: `${truckId}-full-path`,
              points: [currentPosition, ...remainingPoints],
              stroke: getTruckColorById(truckId),
              strokeWidth: 0.7,
              type: 'path' as const,
            },
          ]
        })
      : []
    const roadblockPolylines =
      network?.roadblocks?.map((block, index) => ({
        id: `roadblock-${index}`,
        points: block.blockedNodes?.map(
          (node) => [node.x, node.y] as [number, number],
        ),
        strokeWidth: 1.5,
        type: 'roadblock' as const,
        startTime: block.start,
        endTime: block.end,
      })) || []
    return [...pathPolylines, ...roadblockPolylines]
  }, [
    routes?.paths,
    network?.roadblocks,
    network?.trucks,
    routes?.stops,
    getTruckColorById,
  ])

  return (
    <div className="flex h-full w-full">
      <div className="flex-1 flex flex-col overflow-hidden">
        <SimulationHeader
          simulationTime={simulationTime ?? null}
          isSimulationActive={!!network}
          acceleration={status.timeAcceleration || 1}
        />
        {selectedTruck && (
          <TruckSelectionBanner
            truckCode={selectedTruck.code}
            truckType={selectedTruck.type}
            onDeselect={() => navigate({ search: {} })}
          />
        )}
        <div className="flex-1 overflow-auto">
          <DynamicMap
            trucks={
              network?.trucks.filter(
                (truck) => truck.status !== TruckState.IDLE,
              ) || []
            }
            stations={network?.stations || []}
            orders={
              network?.orders.filter(
                (order) =>
                  order.status !== 'COMPLETED' &&
                  simulationTime &&
                  order.date <= simulationTime,
              ) || []
            }
            polylines={
              poliLines.filter(
                (line) =>
                  line.type !== 'roadblock' ||
                  (line.startTime &&
                    line.endTime &&
                    simulationTime &&
                    new Date(line.startTime) <= new Date(simulationTime) &&
                    new Date(line.endTime) >= new Date(simulationTime)),
              ) || []
            }
            hoveredPolylineId={polylineHover}
            onPolylineHover={(lineId) => {
              const isRoadblock = lineId?.startsWith('roadblock-')
              if (lineId === null || lineId === undefined) {
                setPolylineHover(null)
                return
              }
              if (isRoadblock) {
                setPolylineHover(null)
                return
              }
              setPolylineHover(lineId)
            }}
            onPolylineClick={(lineId) => {
              const isRoadblock = lineId?.startsWith('roadblock-')
              if (isRoadblock) {
                setPolylineHover(null)
                return
              }
              const truckId = Number(lineId?.split('-')[0])
              navigate({
                search: {
                  truckId,
                },
              })
            }}
            onStationClick={(stationId) => {
              navigate({
                search: {
                  stationId,
                },
              })
            }}
            onOrderClick={(orderId) => {
              navigate({
                search: {
                  orderId,
                },
              })
            }}
          />
        </div>
      </div>
      <AsideTab />
      <SimulationEndDialog
        open={isOpen}
        onClose={closeDialog}
        reason={endReason}
      />
    </div>
  )
}
