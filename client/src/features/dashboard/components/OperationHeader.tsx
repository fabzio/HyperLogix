import { cn } from '@/lib/utils'
import { Wifi, WifiOff } from 'lucide-react'

interface OperationMetrics {
  fleetUtilizationPercentage: number
  averageFuelConsumptionPerKm: number
  completionPercentage: number
  averageDeliveryTimeMinutes: number
  averageCapacityUtilization: number
  averagePlanificationTimeSeconds: number
  totalDistanceTraveled: number
  deliveryEfficiencyPercentage: number
  totalOrdersProcessed: number
  pendingOrdersCount: number
}

interface OperationHeaderProps {
  simulationTime: string | null
  isOperationActive: boolean
  metrics?: OperationMetrics | null
}

export default function OperationHeader({
  simulationTime,
  isOperationActive,
  metrics,
}: OperationHeaderProps) {
  const currentTimeDate = simulationTime ? new Date(simulationTime) : new Date()

  return (
    <header className="px-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div
            className={cn(
              'w-2 h-2 rounded-full',
              isOperationActive ? 'bg-green-500 animate-pulse' : 'bg-gray-400',
            )}
          />
          {isOperationActive ? (
            <Wifi className="w-7 h-7 text-primary" />
          ) : (
            <WifiOff className="w-7 h-7 text-gray-400" />
          )}
          <p className="text-base font-semibold">
            {isOperationActive ? 'Operación en tiempo real' : 'Desconectado'}
          </p>
          {metrics && (
            <div className="flex items-center gap-4 ml-4">
              <span className="text-sm text-muted-foreground">
                Entregas completadas: {metrics.totalOrdersProcessed}
              </span>
              <span className="text-sm text-muted-foreground">
                Pendientes: {metrics.pendingOrdersCount}
              </span>
              <span className="text-sm text-muted-foreground">
                Eficiencia: {metrics.deliveryEfficiencyPercentage.toFixed(1)}%
              </span>
            </div>
          )}
        </div>
        <div className="text-right">
          <p className="text-md font-mono font-bold text-primary">
            {currentTimeDate.toLocaleDateString('es-ES', {
              weekday: 'long',
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })}
          </p>
          <p className="text-xl font-mono text-muted-foreground">
            {currentTimeDate.toLocaleTimeString('es-ES', {
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            })}
          </p>
        </div>
      </div>
    </header>
  )
}
