import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { BarChart2, Play, Truck } from 'lucide-react'
import { useState } from 'react'
import AsideTab from './components/AsideTab'
import SimulationHeader from './components/SimulationHeader'
import { useWatchSimulation } from './hooks/useSimulation'

const TABS = [
  { key: 'run', icon: <Play />, label: 'Ejecutar' },
  { key: 'metrics', icon: <BarChart2 />, label: 'Metricas' },
  { key: 'truck', icon: <Truck />, label: 'Camiones' },
]

export default function Simulation() {
  const { plgNetwork: network, simulationTime } = useWatchSimulation()
  const [openTab, setOpenTab] = useState<string | null>(null)

  const poliLines: MapPolyline[] = []

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
            orders={network?.orders || []}
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
    </div>
  )
}
