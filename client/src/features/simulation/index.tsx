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
  const { plgNetwork: network, simulationTime, routes } = useWatchSimulation()
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

          // Junta todos los puntos en un solo path lineal
          const fullPath: [number, number][][] = paths.map(
            (path) =>
              path.points?.map((p) => [p.x, p.y] as [number, number]) || [],
          ) ?? [[]]
          for (let i = 0; i < fullPath.length; i++) {
            fullPath[i].shift()
          }
          const startPath = [truckLocation?.x, truckLocation?.y] as [
            number,
            number,
          ]

          let startIndex = 0
          for (let i = 0; i < fullPath.length; i++) {
            const pathPoints = fullPath[i] || []
            if (routes.stops[truckId]?.[i + 1].arrived) {
              startIndex += pathPoints?.length + 1
              continue
            }
            for (let j = 0; j < pathPoints.length - 1; j++) {
              if (pathPoints[j][0] === pathPoints[j + 1][0]) {
                const isInsegment =
                  truckLocation?.x === pathPoints[j][0] &&
                  truckLocation?.y >= pathPoints[j][1] &&
                  truckLocation?.y <= pathPoints[j + 1][1]
                if (isInsegment) {
                  startIndex += j
                  break
                }
              } else if (pathPoints[j][1] === pathPoints[j + 1][1]) {
                const isInsegment =
                  truckLocation?.y === pathPoints[j][1] &&
                  truckLocation?.x >= pathPoints[j][0] &&
                  truckLocation?.x <= pathPoints[j + 1][0]
                if (isInsegment) {
                  startIndex += j
                  break
                }
              }
            }
          }

          return [
            {
              id: `${truckId}-full-path`,
              points: [startPath, ...fullPath.flat().slice(startIndex)],
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
