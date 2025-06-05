import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { BarChart2, Fuel, Play, Receipt, Route, Truck } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import AsideTab from './components/AsideTab'
import SimulationHeader from './components/SimulationHeader'
import { useWatchSimulation } from './hooks/useSimulation'
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
  const { plgNetwork: network, simulationTime ,routes} = useWatchSimulation()
  const [openTab, setOpenTab] = useState<string | null>(null)
  const [isOpen, setIsOpen] = useState(false)
  const [endReason, setEndReason] = useState<'completed' | 'manual' | null>(null)

  const wasActiveRef = useRef(false)
  const prevAllCompletedRef = useRef(false);
  useEffect(() => {
    if (!network?.orders?.length) return

    const allCompleted = network.orders.every(order => order.status === 'COMPLETED')
    if (allCompleted && !prevAllCompletedRef.current) {
      setEndReason('completed')
      setIsOpen(true)
    }
    prevAllCompletedRef.current = allCompleted;
  }, [network?.orders])

  useEffect(() => {
    const isActive = !!network
    if (wasActiveRef.current && !isActive) {
      setEndReason('manual')
      setIsOpen(true) // 👈 abrir modal de "simulación detenida manualmente"
    }
    wasActiveRef.current = isActive
  }, [network])

  const poliLines: MapPolyline[] = routes?.paths ? 
    Object.entries(routes.paths).flatMap(([truckId, paths]) => 
      paths.map((path, pathIndex) => ({
        id: `${truckId}-path-${pathIndex}`,
        points: path.points?.map(location => [location.x, location.y] as [number, number]) || [],
        stroke: `hsl(${(truckId.charCodeAt(0) * 137.5) % 360}, 70%, 50%)`, // Generate unique color per truck
        strokeWidth: 0.7,
        type: 'path' as const
      }))
    ) : []

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
            orders={network?.orders.filter(order => order.status != 'COMPLETED' && order.date <= simulationTime!) || []}
            polylines={poliLines}
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
      <SimulationEndDialog open={isOpen} onClose={() => setIsOpen(false)} reason={endReason}/>
    </div>
  )
}
