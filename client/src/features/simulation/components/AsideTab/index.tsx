import { Tabs, TabsContent } from '@/components/ui/tabs'
import Metrics from './Metrics'
import Run from './Run'

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
      </div>
    </Tabs>
  )
}
