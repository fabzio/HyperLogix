import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Activity, AlertTriangle, Shield, Wifi, WifiOff } from 'lucide-react'
import { useOperationStore } from '../store/operation'

export function SystemStatus() {
  const { isConnected, plgNetwork, planificationStatus, operationStartTime } =
    useOperationStore()

  // Calculate uptime
  const uptime = operationStartTime
    ? Math.floor(
        (Date.now() - new Date(operationStartTime).getTime()) / (1000 * 60),
      )
    : 0

  // System health indicators
  const systemItems = [
    {
      name: 'Conexión en Tiempo Real',
      status: isConnected ? 'Conectado' : 'Desconectado',
      statusType: isConnected ? 'success' : 'error',
      icon: isConnected ? Wifi : WifiOff,
    },
    {
      name: 'Motor de Planificación',
      status: planificationStatus?.planning ? 'Procesando' : 'En Espera',
      statusType: planificationStatus?.planning ? 'warning' : 'success',
      icon: Activity,
    },
    {
      name: 'Red PLG',
      status: plgNetwork ? 'Activa' : 'Sin Datos',
      statusType: plgNetwork ? 'success' : 'warning',
      icon: Shield,
    },
    {
      name: 'Tiempo de Operación',
      status: `${uptime} min`,
      statusType: 'success',
      icon: Activity,
    },
  ]

  // Check for alerts
  const brokenTrucks =
    plgNetwork?.trucks?.filter((truck) => truck.status === 'BROKEN_DOWN')
      .length ?? 0
  const maintenanceTrucks =
    plgNetwork?.trucks?.filter((truck) => truck.status === 'MAINTENANCE')
      .length ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Shield className="h-5 w-5 text-green-500" />
          Estado del Sistema
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {systemItems.map((item) => {
            const colorClass =
              item.statusType === 'success'
                ? 'green'
                : item.statusType === 'warning'
                  ? 'amber'
                  : 'red'
            const Icon = item.icon

            return (
              <div
                key={item.name}
                className="flex justify-between items-center"
              >
                <div className="flex items-center gap-2">
                  <Icon className={`w-4 h-4 text-${colorClass}-500`} />
                  <span className="text-sm">{item.name}</span>
                </div>
                <Badge
                  variant="outline"
                  className={`bg-${colorClass}-500/10 text-${colorClass}-500 border-${colorClass}-500/20`}
                >
                  {item.status}
                </Badge>
              </div>
            )
          })}

          {/* Alert section */}
          {(brokenTrucks > 0 || maintenanceTrucks > 0) && (
            <div className="pt-4 border-t">
              <div className="flex items-center gap-2 mb-2">
                <AlertTriangle className="w-4 h-4 text-amber-500" />
                <span className="text-sm font-medium">Alertas</span>
              </div>
              <div className="space-y-2">
                {brokenTrucks > 0 && (
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">
                      Camiones Averiados
                    </span>
                    <Badge variant="destructive">{brokenTrucks}</Badge>
                  </div>
                )}
                {maintenanceTrucks > 0 && (
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">
                      En Mantenimiento
                    </span>
                    <Badge
                      variant="outline"
                      className="bg-amber-500/10 text-amber-500 border-amber-500/20"
                    >
                      {maintenanceTrucks}
                    </Badge>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Planning status detail */}
          {planificationStatus?.planning && (
            <div className="pt-4 border-t">
              <div className="flex items-center gap-2 mb-2">
                <Activity className="w-4 h-4 text-blue-500" />
                <span className="text-sm font-medium">Planificación</span>
              </div>
              <div className="text-xs text-muted-foreground">
                Procesando {planificationStatus.currentNodesProcessed} nodos
              </div>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
