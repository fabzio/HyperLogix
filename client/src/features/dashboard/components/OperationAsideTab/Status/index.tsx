import Typography from '@/components/typography'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { CheckCircle2, Clock, Database, Wifi, WifiOff, Zap } from 'lucide-react'
import { useWatchOperation } from '../../../hooks/useOperation'

export default function Status() {
  const {
    isConnected,
    plgNetwork,
    simulationTime,
    planificationStatus,
    operationStartTime,
  } = useWatchOperation()

  const getSystemHealth = () => {
    if (!isConnected)
      return { status: 'disconnected', color: 'red', icon: WifiOff }
    if (!plgNetwork)
      return { status: 'connecting', color: 'yellow', icon: Clock }
    return { status: 'healthy', color: 'green', icon: CheckCircle2 }
  }

  const systemHealth = getSystemHealth()

  const getUptime = () => {
    if (!operationStartTime) return 'N/A'
    const now = new Date()
    const start = new Date(operationStartTime)
    const diffMs = now.getTime() - start.getTime()
    const hours = Math.floor(diffMs / (1000 * 60 * 60))
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
    return `${hours}h ${minutes}m`
  }

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b">
        <Typography variant="h3">Estado del Sistema</Typography>
        <p className="text-sm text-muted-foreground">
          Monitoreo de conectividad y rendimiento
        </p>
      </div>

      <div className="flex-1 overflow-auto p-4 space-y-4">
        {/* Estado de conexión */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm">
              {isConnected ? (
                <Wifi className="h-4 w-4 text-green-600" />
              ) : (
                <WifiOff className="h-4 w-4 text-red-600" />
              )}
              Conectividad
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <div
                className={cn(
                  'w-2 h-2 rounded-full',
                  isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500',
                )}
              />
              <span className="text-sm font-medium">
                {isConnected ? 'Conectado' : 'Desconectado'}
              </span>
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              {isConnected
                ? 'Recibiendo datos en tiempo real'
                : 'Sin conexión con el servidor'}
            </p>
          </CardContent>
        </Card>

        {/* Estado del sistema */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm">
              <systemHealth.icon
                className={`h-4 w-4 text-${systemHealth.color}-600`}
              />
              Salud del Sistema
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-sm">Estado:</span>
                <Badge
                  variant="outline"
                  className={cn(
                    systemHealth.status === 'healthy' &&
                      'border-green-500 text-green-700',
                    systemHealth.status === 'connecting' &&
                      'border-yellow-500 text-yellow-700',
                    systemHealth.status === 'disconnected' &&
                      'border-red-500 text-red-700',
                  )}
                >
                  {systemHealth.status === 'healthy' && 'Operativo'}
                  {systemHealth.status === 'connecting' && 'Conectando'}
                  {systemHealth.status === 'disconnected' && 'Desconectado'}
                </Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Tiempo activo:</span>
                <span className="text-sm font-mono">{getUptime()}</span>
              </div>
              {simulationTime && (
                <div className="flex justify-between items-center">
                  <span className="text-sm">Tiempo actual:</span>
                  <span className="text-xs font-mono">
                    {new Date(simulationTime).toLocaleTimeString('es-ES')}
                  </span>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Estado de planificación */}
        {planificationStatus && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm">
                <Zap className="h-4 w-4 text-blue-600" />
                Planificación
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Estado:</span>
                  <div className="flex items-center gap-2">
                    {planificationStatus.planning && (
                      <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                    )}
                    <span className="text-sm">
                      {planificationStatus.planning ? 'Calculando' : 'Inactivo'}
                    </span>
                  </div>
                </div>
                {planificationStatus.planning && (
                  <div className="flex justify-between items-center">
                    <span className="text-sm">Nodos procesados:</span>
                    <span className="text-sm font-mono">
                      {planificationStatus.currentNodesProcessed}
                    </span>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Recursos del sistema */}
        {plgNetwork && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm">
                <Database className="h-4 w-4 text-purple-600" />
                Recursos
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Camiones:</span>
                  <span className="text-sm font-medium">
                    {
                      plgNetwork.trucks.filter((t) => t.status !== 'IDLE')
                        .length
                    }{' '}
                    / {plgNetwork.trucks.length}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Estaciones:</span>
                  <span className="text-sm font-medium">
                    {plgNetwork.stations.length}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Órdenes activas:</span>
                  <span className="text-sm font-medium">
                    {
                      plgNetwork.orders.filter((o) => o.status !== 'COMPLETED')
                        .length
                    }
                  </span>
                </div>
                {plgNetwork.roadblocks && plgNetwork.roadblocks.length > 0 && (
                  <div className="flex justify-between items-center">
                    <span className="text-sm">Bloqueos:</span>
                    <span className="text-sm font-medium text-red-600">
                      {plgNetwork.roadblocks.length}
                    </span>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
