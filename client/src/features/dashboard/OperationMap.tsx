import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { TruckSelectionBanner } from '@/components/TruckSelectionBanner'
import { TruckState } from '@/domain/TruckState'
import OperationAsideTab from '@/features/dashboard/components/OperationAsideTab'
import OperationHeader from '@/features/dashboard/components/OperationHeader'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useWatchOperation } from './hooks/useOperation'

const truckTypeColors: Record<string, string> = {
  TA: '#a855f7', // purple-500
  TB: '#38bdf8', // sky-400
  TC: '#4ade80', // green-400
  TD: '#facc15', // yellow-400
}

export default function OperationMap() {
  const [polylineHover, setPolylineHover] = useState<string | null>(null)
  const { truckId } = useSearch({ from: '/_auth/map' })
  const navigate = useNavigate({ from: '/map' })

  // Use operation hooks for real-time data
  const {
    plgNetwork: network,
    simulationTime,
    routes,
    isConnected,
    metrics,
  } = useWatchOperation()

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

  useEffect(() => {
    if (truckId) {
      setPolylineHover(`${truckId}-path-0`)
    } else {
      setPolylineHover(null)
    }
  }, [truckId])

  const polylines: MapPolyline[] = useMemo(() => {
    const pathPolylines = routes?.paths
      ? Object.entries(routes.paths).flatMap(([truckId, paths]) => {
          const truckLocation = network?.trucks.find(
            (t) => t.id === truckId,
          )?.location

          // Junta todos los puntos en un solo path lineal
          const fullPath: [number, number][][] = paths.map(
            (path) =>
              path.points?.map((p) => [p.x, p.y] as [number, number]) || [],
          )
          for (let i = 1; i < fullPath.length; i++) {
            fullPath[i].shift()
          }
          const startPath = [truckLocation?.x, truckLocation?.y] as [
            number,
            number,
          ]

          let startIndex = 0
          console.log('fullPath', fullPath)
          for (let i = 0; i < fullPath.length; i++) {
            const pathPoints = fullPath[i] || []
            if (routes.stops[truckId]?.[i + 1].arrived) {
              startIndex += pathPoints[i].length
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
              points: [startPath, ...fullPath.flat().slice(startIndex + 1)],
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
        <OperationHeader
          simulationTime={simulationTime ?? null}
          isOperationActive={isConnected && !!network}
          metrics={metrics}
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
                (order) => order.status !== 'COMPLETED' && simulationTime,
              ) || []
            }
            polylines={
              polylines.filter((line) => {
                if (line.type === 'path') return true
                if (
                  line.type === 'roadblock' &&
                  line.startTime &&
                  line.endTime &&
                  simulationTime
                ) {
                  return (
                    new Date(line.startTime) <= new Date(simulationTime) &&
                    new Date(line.endTime) >= new Date(simulationTime)
                  )
                }
                return false
              }) || []
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
                const roadblockId = Number(lineId.split('-')[1])
                const roadblock = network?.roadblocks?.[roadblockId]
                if (roadblock?.start) {
                  navigate({
                    search: {
                      roadblockStart: roadblock.start,
                    },
                  })
                }
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
      <OperationAsideTab />
    </div>
  )
}
