import { Tabs, TabsContent } from '@/components/ui/tabs'
import Metrics from './Metrics'
import Orders from './Orders'
import Routes from './Routes'
import Run from './Run'
import Trucks from './Trucks'

interface AsideTabProps {
  openTab: string
}

export default function AsideTab({ openTab }: AsideTabProps) {
  return (
    <Tabs
      value={openTab}
      orientation="vertical"
      className="h-full flex flex-col"
    >
      <div className="flex-1 p-6">
        <TabsContent value="run" className="flex flex-col gap-4">
          <Run />
        </TabsContent>
        <TabsContent value="metrics">
          <Metrics />
        </TabsContent>
        <TabsContent value="truck">
          <Trucks />
        </TabsContent>
        <TabsContent value="orders">
          <Orders />
        </TabsContent>
        <TabsContent value="routes">
          <Routes />
        </TabsContent>
      </div>
    </Tabs>
  )
}
