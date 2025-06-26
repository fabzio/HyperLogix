import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import AddOrderDialog from '@/features/dashboard/components/AddOrderDialog'
import { useWatchOperation } from '@/features/dashboard/hooks/useOperation'
import { useOperationStatus } from '@/features/dashboard/hooks/useOperationMutations'
import { Activity, Clock, Package, Users } from 'lucide-react'

export default function Clients() {
  const operation = useWatchOperation()
  const { data: operationStatus } = useOperationStatus()

  // Get pending orders from PLG network
  const pendingOrders =
    operation.plgNetwork?.orders?.filter(
      (order) =>
        order.status === 'PENDING' ||
        order.status === 'CALCULATING' ||
        order.status === 'IN_PROGRESS',
    ) || []

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Operación en Tiempo Real
          </h1>
          <p className="text-muted-foreground">
            Registre nuevas órdenes y monitoree el estado de la operación
          </p>
        </div>
        <AddOrderDialog />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Estado del Sistema
            </CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center space-x-2">
              <Badge
                variant={operation.isConnected ? 'default' : 'destructive'}
              >
                {operation.isConnected ? 'Conectado' : 'Desconectado'}
              </Badge>
              <span className="text-sm text-muted-foreground">
                {operation.simulationTime ? 'En operación' : 'Iniciando...'}
              </span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Órdenes Pendientes
            </CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{pendingOrders.length}</div>
            <p className="text-xs text-muted-foreground">
              En cola de procesamiento
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Última Orden</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-sm font-medium">
              {operation.lastOrderSubmitted?.id || 'Ninguna'}
            </div>
            <p className="text-xs text-muted-foreground">
              {operation.lastOrderSubmitted?.clientId ||
                'Sin órdenes registradas'}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Estado de Envío
            </CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center space-x-2">
              <Badge
                variant={operation.isSubmittingOrder ? 'secondary' : 'outline'}
              >
                {operation.isSubmittingOrder ? 'Enviando...' : 'Listo'}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {operation.simulationTime && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Información de la Simulación</CardTitle>
            <CardDescription>
              Estado actual de la operación en tiempo real
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <span className="text-sm font-medium">
                  Tiempo de Simulación:
                </span>
                <p className="text-sm text-muted-foreground">
                  {operation.simulationTime}
                </p>
              </div>
              <div>
                <span className="text-sm font-medium">Camiones Activos:</span>
                <p className="text-sm text-muted-foreground">
                  {operation.plgNetwork?.trucks?.length || 0}
                </p>
              </div>
              <div>
                <span className="text-sm font-medium">Estaciones:</span>
                <p className="text-sm text-muted-foreground">
                  {operation.plgNetwork?.stations?.length || 0}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {pendingOrders.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Órdenes en Cola</CardTitle>
            <CardDescription>
              Órdenes registradas esperando procesamiento por el sistema
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {pendingOrders.map((order) => (
                <div
                  key={order.id}
                  className="flex items-center justify-between p-3 rounded-lg border bg-card"
                >
                  <div className="flex flex-col">
                    <span className="font-medium">{order.id}</span>
                    <span className="text-sm text-muted-foreground">
                      Cliente: {order.clientId}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      Ubicación: ({order.location.x}, {order.location.y})
                    </span>
                  </div>
                  <div className="flex flex-col items-end">
                    <span className="font-medium">{order.requestedGLP}L</span>
                    <Badge variant="outline" className="text-xs">
                      {order.status}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {operationStatus && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Estado del Servidor</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <p className="text-sm">
                <span className="font-medium">Estado:</span>{' '}
                {operationStatus.systemStatus}
              </p>
              <p className="text-sm">
                <span className="font-medium">Sesión:</span>{' '}
                {operationStatus.sessionId}
              </p>
              <p className="text-sm">
                <span className="font-medium">Inicializado:</span>{' '}
                <Badge
                  variant={
                    operationStatus.simulationInitialized
                      ? 'default'
                      : 'secondary'
                  }
                >
                  {operationStatus.simulationInitialized ? 'Sí' : 'No'}
                </Badge>
              </p>
              <p className="text-sm text-muted-foreground">
                {operationStatus.message}
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
