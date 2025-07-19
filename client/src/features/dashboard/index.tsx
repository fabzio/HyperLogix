import { DashboardHeader } from './components/DashboardHeader'
import { FuelStatus } from './components/FuelStatus'
import { MetricCards } from './components/MetricCards'
import OrdersList from './components/OrdersList'
import { QuickActions } from './components/QuickActions'
import { SpeedControls } from './components/SpeedControls'
import { SystemAlerts } from './components/SystemAlerts'
import { SystemStatus } from './components/SystemStatus'
import TrucksList from './components/TrucksList'
import { useWatchOperation } from './hooks/useOperation'

export default function DashboardFeature() {
  const operation = useWatchOperation()

  // Start watching the operation automatically
  // The WebSocket connection is managed by the useWatchOperation hook

  return (
    <div className="container mx-auto px-4 py-6 space-y-6">
      <DashboardHeader
        currentTime={
          operation.simulationTime
            ? new Date(operation.simulationTime)
            : new Date()
        }
      />

      {/* Main dashboard grid with real-time metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Main content area - spans 3 columns on large screens */}
        <div className="lg:col-span-3 space-y-6">
          <MetricCards />
          {/* Replace charts with truck and order lists */}
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
            <TrucksList trucks={operation.plgNetwork?.trucks || []} />
            <OrdersList
              orders={operation.plgNetwork?.orders || []}
              simulationTime={operation.simulationTime || undefined}
              routes={operation.routes || undefined}
            />
          </div>
        </div>

        {/* Sidebar - spans 1 column */}
        <div className="space-y-6">
          <SystemStatus />
          <SpeedControls />
          <QuickActions />
          <SystemAlerts />
          <FuelStatus />
        </div>
      </div>

      {/* Connection status indicator */}
      {!operation.isConnected && (
        <div className="fixed bottom-4 right-4 bg-red-500 text-white px-4 py-2 rounded-lg shadow-lg">
          ⚠️ Sistema desconectado
        </div>
      )}
    </div>
  )
}
