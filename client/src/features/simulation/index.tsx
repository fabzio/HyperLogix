import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { BarChart2, Fuel, Play, Receipt, Route, Truck } from 'lucide-react'
import { useState, useMemo } from 'react'
import AsideTab from './components/AsideTab'
import SimulationHeader from './components/SimulationHeader'
import { useSimulationEndDialog, useWatchSimulation } from './hooks/useSimulation'
import SimulationEndDialog from './components/SimulationEndDialog'


const TABS = [
  { key: 'run', icon: <Play />, label: 'Ejecutar' },
  { key: 'metrics', icon: <BarChart2 />, label: 'Metricas' },
  { key: 'truck', icon: <Truck />, label: 'Camiones' },
  { key: 'orders', icon: <Receipt />, label: 'Pedidos' },
  { key: 'routes', icon: <Route />, label: 'Rutas' },
  { key: 'stations', icon: <Fuel />, label: 'Estaciones' },
]

export default function Simulation() {
  const { plgNetwork: network, simulationTime, routes } = useWatchSimulation()
  const [openTab, setOpenTab] = useState<string | null>(null)
  const { isOpen, endReason, closeDialog } = useSimulationEndDialog(network)

  const poliLines: MapPolyline[] = useMemo(() => {
    const pathPolylines = routes?.paths
      ? Object.entries(routes.paths).flatMap(([truckId, paths]) =>
        paths.map((path, pathIndex) => ({
          id: `${truckId}-path-${pathIndex}`,
          points:
            path.points?.map(
              (location) => [location.x, location.y] as [number, number],
            ) || [],
          stroke: `hsl(${(truckId.charCodeAt(0) * 137.5) % 360}, 70%, 50%)`,
          strokeWidth: 0.7,
          type: 'path' as const,
        })),
      )
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
  }, [routes?.paths, network?.roadblocks])

  return (
    <div className="flex h-full w-full">
      <div className="flex-1 flex flex-col overflow-hidden">
        <SimulationHeader
          simulationTime={simulationTime ?? null}
          isSimulationActive={!!network}
        />
        <div className="flex-1 overflow-auto">
          <DynamicMap
            trucks={network?.trucks || []}
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
          />
        </div>
      </div>

      {openTab && (
        <aside className="w-80 border-l bg-background flex flex-col h-full shadow-lg">
          <AsideTab openTab={openTab} />
        </aside>
      )}

      <div className="flex flex-col gap-2 bg-background border-l shadow-lg p-1.5 items-center h-full">
        {TABS.map((tab) => {
          const isActive = openTab === tab.key
          return (
            <Button
              size="icon"
              variant="ghost"
              key={tab.key}
              onClick={() => setOpenTab(isActive ? null : tab.key)}
              className={cn(
                'flex flex-col items-center justify-center rounded-md transition-colors p-2',
                isActive
                  ? 'bg-muted text-foreground'
                  : 'text-muted-foreground hover:bg-muted/60',
              )}
              type="button"
              title={tab.label}
            >
              {tab.icon}
            </Button>
          )
        })}
      </div>
      <SimulationEndDialog open={isOpen} onClose={closeDialog} reason={endReason} />
    </div>
  )
}
