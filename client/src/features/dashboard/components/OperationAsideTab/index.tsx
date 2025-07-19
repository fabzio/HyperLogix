import { Button } from '@/components/ui/button'
import { Tabs, TabsContent } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { useSearch } from '@tanstack/react-router'
import { Activity, BarChart2, Fuel, Package, Truck } from 'lucide-react'
import { useEffect, useState } from 'react'
import Metrics from './Metrics'
import Orders from './Orders'
import Stations from './Stations'
import Status from './Status'
import Trucks from './Trucks'

export default function OperationAsideTab() {
  const [openTab, setOpenTab] = useState<string>()
  const { orderId, truckId, stationId } = useSearch({
    from: '/_auth/map',
  })

  useEffect(() => {
    if (orderId) {
      setOpenTab('orders')
    } else if (truckId) {
      setOpenTab('trucks')
    } else if (stationId) {
      setOpenTab('stations')
    }
  }, [orderId, truckId, stationId])

  return (
    <Tabs
      value={openTab}
      onValueChange={setOpenTab}
      orientation="vertical"
      className="flex flex-row"
    >
      {TABS.map((tab) => (
        <TabsContent key={tab.key} value={tab.key} className="px-2">
          <tab.component />
        </TabsContent>
      ))}
      <div className="flex flex-col gap-2 bg-background border-l shadow-lg p-1.5 items-center h-full">
        {TABS.map((tab) => {
          const isActive = openTab === tab.key
          return (
            <Button
              size="icon"
              variant="ghost"
              key={tab.key}
              onClick={() => setOpenTab(isActive ? undefined : tab.key)}
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
    </Tabs>
  )
}

const TABS = [
  { key: 'trucks', icon: <Truck />, label: 'Camiones', component: Trucks },
  { key: 'orders', icon: <Package />, label: 'Órdenes', component: Orders },
  { key: 'stations', icon: <Fuel />, label: 'Estaciones', component: Stations },
  {
    key: 'metrics',
    icon: <BarChart2 />,
    label: 'Métricas',
    component: Metrics,
  },
  { key: 'status', icon: <Activity />, label: 'Estado', component: Status },
]
