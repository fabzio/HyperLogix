import type { MapPolyline } from '@/components/DynamicMap'
import DynamicMap from '@/components/DynamicMap'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { BarChart2, Play, Truck } from 'lucide-react'
import { useState } from 'react'
import AsideTab from './components/AsideTab'
import { useWatchSimulation } from './hooks/useSimulation'

const TABS = [
  { key: 'run', icon: <Play />, label: 'Ejecutar' },
  { key: 'metrics', icon: <BarChart2 />, label: 'Metricas' },
  { key: 'truck', icon: <Truck />, label: 'Camiones' },
]

export default function Simulation() {
  const { network } = useWatchSimulation()
  const [openTab, setOpenTab] = useState<string | null>(null)

  const poliLines: MapPolyline[] = []
  const pointMarkers: [number, number][] = []

  return (
    <div className="flex h-screen w-full">
      <div className="flex-1 relative">
        <DynamicMap
          trucks={network?.trucks || []}
          stations={network?.stations || []}
          orders={network?.orders || []}
          polylines={poliLines}
        />
      </div>

      {openTab && (
        <aside className="w-80 border-l bg-background flex flex-col h-full shadow-lg">
          <AsideTab openTab={openTab} />
        </aside>
      )}

      <div className="flex flex-col gap-2 bg-background/95 border-l shadow-lg p-1.5 items-center h-full">
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

const getColorFromKey = (key: string): string => {
  const hash = Array.from(key).reduce(
    (acc, char) => acc + char.charCodeAt(0),
    0,
  )
  const hue = hash % 360 // Generate a hue value between 0 and 359
  return `hsl(${hue}, 80%, 40%)` // Adjust saturation to 80% and lightness to 40% for better visibility
}
