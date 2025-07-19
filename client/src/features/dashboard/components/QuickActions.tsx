import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  AlertTriangle,
  FileText,
  RefreshCcw,
  Route as RouteIcon,
} from 'lucide-react'
import { toast } from 'sonner'
import { useManualReplanification } from '../hooks/useOperationMutations'
import { useOperationStore } from '../store/operation'
import AddOrderDialog from './AddOrderDialog'
import { CreateBlockadeDialog } from './CreateBlockadeDialog'
import { MapModal } from './MapModal'

export function QuickActions() {
  const { isConnected, planificationStatus, plgNetwork, metrics } =
    useOperationStore()
  const manualReplanificationMutation = useManualReplanification()

  const handleOptimizeRoutes = async () => {
    if (!isConnected) {
      toast.error('Sistema desconectado. No se pueden optimizar rutas.')
      return
    }

    if (planificationStatus?.planning) {
      toast.warning('Ya hay una optimización en progreso.')
      return
    }

    try {
      await manualReplanificationMutation.mutateAsync()
      toast.success('Replanificación manual iniciada exitosamente.')
    } catch (error) {
      toast.error('Error al iniciar la replanificación manual.')
      console.error('Error optimizing routes:', error)
    }
  }

  const handleExportData = () => {
    if (!metrics || !plgNetwork) {
      console.log('No hay datos disponibles para exportar.')
      return
    }

    // Crear reporte HTML que luego se puede convertir a PDF
    const reportData = {
      timestamp: new Date().toLocaleString('es-ES'),
      date: new Date().toLocaleDateString('es-ES'),
      metrics,
      fleet: {
        total: plgNetwork.trucks?.length ?? 0,
        active:
          plgNetwork.trucks?.filter((t) => t.status === 'ACTIVE').length ?? 0,
        idle: plgNetwork.trucks?.filter((t) => t.status === 'IDLE').length ?? 0,
        broken:
          plgNetwork.trucks?.filter((t) => t.status === 'BROKEN_DOWN').length ??
          0,
      },
      orders: {
        total: plgNetwork.orders?.length ?? 0,
        pending:
          plgNetwork.orders?.filter((o) => o.status === 'PENDING').length ?? 0,
        inProgress:
          plgNetwork.orders?.filter((o) => o.status === 'IN_PROGRESS').length ??
          0,
        completed:
          plgNetwork.orders?.filter((o) => o.status === 'COMPLETED').length ??
          0,
      },
      stations: plgNetwork.stations?.length ?? 0,
    }

    // Generar HTML del reporte
    const htmlContent = `
			<!DOCTYPE html>
			<html>
			<head>
				<meta charset="UTF-8">
				<title>Reporte HyperLogix - ${reportData.date}</title>
				<style>
					body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
					.header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }
					.section { margin-bottom: 25px; }
					.section h2 { color: #333; border-bottom: 1px solid #ddd; padding-bottom: 5px; }
					.metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
					.metric-card { border: 1px solid #ddd; padding: 15px; border-radius: 8px; }
					.metric-value { font-size: 24px; font-weight: bold; color: #0066cc; }
					.metric-label { color: #666; font-size: 14px; }
					.footer { margin-top: 40px; text-align: center; color: #666; font-size: 12px; }
					table { width: 100%; border-collapse: collapse; margin-top: 10px; }
					th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
					th { background-color: #f5f5f5; }
				</style>
			</head>
			<body>
				<div class="header">
					<h1>🚛 HyperLogix - Reporte Operacional</h1>
					<p>Generado el ${reportData.timestamp}</p>
				</div>

				<div class="section">
					<h2>📊 Métricas del Sistema</h2>
					<div class="metrics-grid">
						<div class="metric-card">
							<div class="metric-value">${reportData.metrics.fleetUtilizationPercentage.toFixed(1)}%</div>
							<div class="metric-label">Utilización de Flota</div>
						</div>
						<div class="metric-card">
							<div class="metric-value">${reportData.metrics.completionPercentage.toFixed(1)}%</div>
							<div class="metric-label">Órdenes Completadas</div>
						</div>
						<div class="metric-card">
							<div class="metric-value">${reportData.metrics.averageFuelConsumptionPerKm.toFixed(2)}</div>
							<div class="metric-label">Consumo Promedio (gal/km)</div>
						</div>
						<div class="metric-card">
							<div class="metric-value">${reportData.metrics.totalDistanceTraveled.toFixed(1)} km</div>
							<div class="metric-label">Distancia Total Recorrida</div>
						</div>
					</div>
				</div>

				<div class="section">
					<h2>🚛 Estado de la Flota</h2>
					<table>
						<tr><th>Estado</th><th>Cantidad</th><th>Porcentaje</th></tr>
						<tr><td>Activos</td><td>${reportData.fleet.active}</td><td>${((reportData.fleet.active / reportData.fleet.total) * 100).toFixed(1)}%</td></tr>
						<tr><td>Inactivos</td><td>${reportData.fleet.idle}</td><td>${((reportData.fleet.idle / reportData.fleet.total) * 100).toFixed(1)}%</td></tr>
						<tr><td>Averiados</td><td>${reportData.fleet.broken}</td><td>${((reportData.fleet.broken / reportData.fleet.total) * 100).toFixed(1)}%</td></tr>
						<tr><td><strong>Total</strong></td><td><strong>${reportData.fleet.total}</strong></td><td><strong>100%</strong></td></tr>
					</table>
				</div>

				<div class="section">
					<h2>📦 Estado de Órdenes</h2>
					<table>
						<tr><th>Estado</th><th>Cantidad</th><th>Porcentaje</th></tr>
						<tr><td>Pendientes</td><td>${reportData.orders.pending}</td><td>${((reportData.orders.pending / reportData.orders.total) * 100).toFixed(1)}%</td></tr>
						<tr><td>En Progreso</td><td>${reportData.orders.inProgress}</td><td>${((reportData.orders.inProgress / reportData.orders.total) * 100).toFixed(1)}%</td></tr>
						<tr><td>Completadas</td><td>${reportData.orders.completed}</td><td>${((reportData.orders.completed / reportData.orders.total) * 100).toFixed(1)}%</td></tr>
						<tr><td><strong>Total</strong></td><td><strong>${reportData.orders.total}</strong></td><td><strong>100%</strong></td></tr>
					</table>
				</div>

				<div class="section">
					<h2>⛽ Información Adicional</h2>
					<p><strong>Estaciones disponibles:</strong> ${reportData.stations}</p>
					<p><strong>Tiempo promedio de entrega:</strong> ${reportData.metrics.averageDeliveryTimeMinutes.toFixed(1)} minutos</p>
					<p><strong>Utilización promedio de capacidad:</strong> ${reportData.metrics.averageCapacityUtilization.toFixed(1)}%</p>
					<p><strong>Eficiencia de entrega:</strong> ${reportData.metrics.deliveryEfficiencyPercentage.toFixed(1)}%</p>
				</div>

				<div class="footer">
					<p>Reporte generado automáticamente por HyperLogix</p>
					<p>© 2025 HyperLogix - Sistema de Gestión de Flota de GLP</p>
				</div>
			</body>
			</html>
		`

    // Crear y descargar el archivo HTML
    const blob = new Blob([htmlContent], { type: 'text/html' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `reporte-hyperlogix-${new Date().toISOString().split('T')[0]}.html`
    a.click()
    URL.revokeObjectURL(url)

    console.log(
      'Reporte exportado exitosamente. Puede abrir el archivo HTML e imprimirlo como PDF desde su navegador.',
    )
  }

  const activeTrucks =
    plgNetwork?.trucks?.filter((t) => t.status === 'ACTIVE').length ?? 0
  const pendingOrders =
    plgNetwork?.orders?.filter(
      (o) => o.status === 'PENDING' || o.status === 'CALCULATING',
    ).length ?? 0
  const brokenTrucks =
    plgNetwork?.trucks?.filter((t) => t.status === 'BROKEN_DOWN').length ?? 0

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            Acciones Rápidas
            {isConnected ? (
              <Badge
                variant="outline"
                className="bg-green-500/10 text-green-500 border-green-500/20"
              >
                Conectado
              </Badge>
            ) : (
              <Badge
                variant="outline"
                className="bg-red-500/10 text-red-500 border-red-500/20"
              >
                Desconectado
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Status indicators */}
          <div className="grid grid-cols-3 gap-2 mb-4">
            <div className="text-center">
              <div className="text-lg font-bold text-blue-600">
                {activeTrucks}
              </div>
              <div className="text-xs text-muted-foreground">Activos</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-amber-600">
                {pendingOrders}
              </div>
              <div className="text-xs text-muted-foreground">Pendientes</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-red-600">
                {brokenTrucks}
              </div>
              <div className="text-xs text-muted-foreground">Averiados</div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="grid grid-cols-2 gap-3">
            <AddOrderDialog />

            <Button
              variant="outline"
              onClick={handleOptimizeRoutes}
              className="flex flex-col h-20 items-center justify-center gap-2"
              disabled={
                !isConnected ||
                planificationStatus?.planning ||
                manualReplanificationMutation.isPending
              }
            >
              {planificationStatus?.planning ||
              manualReplanificationMutation.isPending ? (
                <RefreshCcw className="h-5 w-5 text-amber-500 animate-spin" />
              ) : (
                <RouteIcon className="h-5 w-5 text-blue-500" />
              )}
              <span className="text-xs">
                {planificationStatus?.planning ||
                manualReplanificationMutation.isPending
                  ? 'Optimizando...'
                  : 'Optimizar'}
              </span>
            </Button>

            <CreateBlockadeDialog />

            <MapModal asQuickAction={true} />

            <Button
              variant="outline"
              onClick={handleExportData}
              className="flex flex-col h-20 items-center justify-center gap-2"
              disabled={!metrics}
            >
              <FileText className="h-5 w-5 text-purple-500" />
              <span className="text-xs">Reporte</span>
            </Button>
          </div>

          {/* Alerts */}
          {brokenTrucks > 0 && (
            <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
              <AlertTriangle className="h-4 w-4 text-red-500" />
              <span className="text-sm text-red-600">
                {brokenTrucks} camión{brokenTrucks > 1 ? 'es' : ''} averiado
                {brokenTrucks > 1 ? 's' : ''}
              </span>
            </div>
          )}

          {pendingOrders > 5 && (
            <div className="flex items-center gap-2 p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg">
              <AlertTriangle className="h-4 w-4 text-amber-500" />
              <span className="text-sm text-amber-600">
                Alto volumen de pedidos pendientes
              </span>
            </div>
          )}
        </CardContent>
      </Card>
    </>
  )
}
