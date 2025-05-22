import { DashboardHeader } from './components/DashboardHeader'
import { FuelStatus } from './components/FuelStatus'
import { MetricCards } from './components/MetricCards'
import { PerformanceChart } from './components/PerformanceChart'
import { QuickActions } from './components/QuickActions'
import { ResourceAllocation } from './components/ResourceAllocation'
import { SystemAlerts } from './components/SystemAlerts'
import { SystemStatus } from './components/SystemStatus'

export default function DashboardFeature() {
  return (
    <div className="container mx-auto px-4 py-6">
      <DashboardHeader />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <MetricCards />
          <PerformanceChart />
          <ResourceAllocation />
        </div>

        <div className="space-y-6">
          <SystemStatus />
          <QuickActions />
          <SystemAlerts />
          <FuelStatus />
        </div>
      </div>
    </div>
  )
}
