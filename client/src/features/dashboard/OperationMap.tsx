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
          const truck = network?.trucks.find((t) => t.id === truckId)
          return paths.map((path, pathIndex) => {
            let points =
              path.points?.map(
                (location) => [location.x, location.y] as [number, number],
              ) || []

            // If we have the truck's current position, start from there
            if (truck?.location && points.length > 0) {
              const truckPoint: [number, number] = [
                truck.location.x,
                truck.location.y,
              ]

              // Find the closest point in the path to the truck's current position
              let closestIndex = 0
              let minDistance = Number.POSITIVE_INFINITY

              points.forEach((point, index) => {
                const distance = Math.sqrt(
                  (point[0] - truckPoint[0]) ** 2 +
                    (point[1] - truckPoint[1]) ** 2,
                )
                if (distance < minDistance) {
                  minDistance = distance
                  closestIndex = index
                }
              })

              // Create the path from truck position to the end of the route
              points = [truckPoint, ...points.slice(closestIndex)]
            }

            return {
              id: `${truckId}-path-${pathIndex}`,
              points,
              stroke: getTruckColorById(truckId),
              strokeWidth: 0.7,
              type: 'path' as const,
            }
          })
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
  }, [routes?.paths, network?.roadblocks, network?.trucks, getTruckColorById])

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
                (order) =>
                  order.status !== 'COMPLETED' &&
                  simulationTime &&
                  order.date <= simulationTime,
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
