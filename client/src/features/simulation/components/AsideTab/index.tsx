import { Button } from '@/components/ui/button'
import { Tabs, TabsContent } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { BarChart2, Fuel, Play, Receipt, Route, Truck } from 'lucide-react'
import { useState } from 'react'
import Metrics from './Metrics'
import Orders from './Orders'
import Routes from './Routes'
import Run from './Run'
import Stations from './Stations'
import Trucks from './Trucks'

export default function AsideTab() {
  const [openTab, setOpenTab] = useState<string>()

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
  { key: 'run', icon: <Play />, label: 'Ejecutar', component: Run },
  {
    key: 'metrics',
    icon: <BarChart2 />,
    label: 'Métricas',
    component: Metrics,
  },
  { key: 'truck', icon: <Truck />, label: 'Camiones', component: Trucks },
  { key: 'orders', icon: <Receipt />, label: 'Pedidos', component: Orders },
  { key: 'routes', icon: <Route />, label: 'Rutas', component: Routes },
  { key: 'stations', icon: <Fuel />, label: 'Estaciones', component: Stations },
]
