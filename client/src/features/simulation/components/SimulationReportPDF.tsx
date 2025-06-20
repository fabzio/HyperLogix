import type { PLGNetwork } from '@/domain/PLGNetwork'
import {
  Document,
  Image,
  Page,
  StyleSheet,
  Text,
  View,
  pdf,
} from '@react-pdf/renderer'
import { differenceInMinutes, differenceInSeconds, format } from 'date-fns'
import { es } from 'date-fns/locale'

const styles = StyleSheet.create({
  page: {
    flexDirection: 'column',
    backgroundColor: '#ffffff',
    padding: 30,
    fontFamily: 'Helvetica',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 30,
    paddingBottom: 15,
    borderBottomWidth: 2,
    borderBottomColor: '#e5e7eb',
  },
  logo: {
    width: 60,
    height: 60,
  },
  headerText: {
    flex: 1,
    paddingLeft: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 5,
  },
  subtitle: {
    fontSize: 12,
    color: '#6b7280',
  },
  infoSection: {
    marginBottom: 25,
    padding: 15,
    backgroundColor: '#f9fafb',
    borderRadius: 8,
  },
  infoTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#374151',
    marginBottom: 10,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 5,
  },
  infoLabel: {
    fontSize: 11,
    color: '#6b7280',
    flex: 1,
  },
  infoValue: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#1f2937',
    flex: 1,
    textAlign: 'right',
  },
  metricsSection: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 15,
    paddingBottom: 5,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  metricsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  metricCard: {
    width: '48%',
    marginBottom: 15,
    padding: 12,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 6,
  },
  metricTitle: {
    fontSize: 10,
    color: '#6b7280',
    marginBottom: 5,
  },
  metricValue: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#1f2937',
  },
  metricPercentage: {
    fontSize: 12,
    color: '#059669',
    marginTop: 2,
  },
  summarySection: {
    marginTop: 20,
    padding: 15,
    backgroundColor: '#ecfdf5',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#a7f3d0',
  },
  summaryTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#065f46',
    marginBottom: 10,
  },
  summaryText: {
    fontSize: 11,
    color: '#047857',
    lineHeight: 1.5,
  },
  footer: {
    marginTop: 'auto',
    paddingTop: 15,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  footerText: {
    fontSize: 10,
    color: '#6b7280',
  },
})

interface SimulationMetrics {
  fleetUtilizationPercentage: number
  averageFuelConsumptionPerKm: number
  completionPercentage: number
  averageDeliveryTimeMinutes: number
  averageCapacityUtilization: number
  averagePlanificationTimeSeconds: number
  totalDistanceTraveled: number
  deliveryEfficiencyPercentage: number
}

interface ReportProps {
  metrics: SimulationMetrics
  startTime: string
  endTime: string
  username: string
  plgNetwork?: PLGNetwork
}

interface NetworkAnalysis {
  totalOrders: number
  completedOrders: number
  pendingOrders: number
  inProgressOrders: number
  totalGLPRequested: number
  totalGLPDelivered: number
  totalTrucks: number
  activeTrucks: number
  totalFuelConsumed: number
  totalStations: number
  avgTruckCapacityUsed: number
}

const formatDuration = (
  durationMinutes: number,
  durationSeconds: number,
): string => {
  if (durationMinutes < 1) {
    return `${durationSeconds} segundos`
  }
  if (durationMinutes < 60) {
    const remainingSeconds = durationSeconds % 60
    return remainingSeconds > 0
      ? `${durationMinutes} min ${remainingSeconds} seg`
      : `${durationMinutes} minutos`
  }
  const hours = Math.floor(durationMinutes / 60)
  const minutes = durationMinutes % 60
  return minutes > 0 ? `${hours}h ${minutes}min` : `${hours} horas`
}

const getSystemStatus = (completionPercentage: number): string => {
  if (completionPercentage > 80) return 'Óptimo'
  if (completionPercentage > 60) return 'Bueno'
  return 'Mejorable'
}

const calculateNetworkAnalysis = (plgNetwork: PLGNetwork): NetworkAnalysis => ({
  totalOrders: plgNetwork.orders.length,
  completedOrders: plgNetwork.orders.filter(
    (order) => order.status === 'COMPLETED',
  ).length,
  pendingOrders: plgNetwork.orders.filter((order) => order.status === 'PENDING')
    .length,
  inProgressOrders: plgNetwork.orders.filter(
    (order) => order.status === 'IN_PROGRESS',
  ).length,
  totalGLPRequested: plgNetwork.orders.reduce(
    (sum, order) => sum + order.requestedGLP,
    0,
  ),
  totalGLPDelivered: plgNetwork.orders.reduce(
    (sum, order) => sum + order.deliveredGLP,
    0,
  ),
  totalTrucks: plgNetwork.trucks.length,
  activeTrucks: plgNetwork.trucks.filter((truck) => truck.status === 'ACTIVE')
    .length,
  totalFuelConsumed: plgNetwork.trucks.reduce(
    (sum, truck) => sum + (truck.fuelCapacity - truck.currentFuel),
    0,
  ),
  totalStations: plgNetwork.stations.length,
  avgTruckCapacityUsed:
    plgNetwork.trucks.reduce(
      (sum, truck) => sum + (truck.currentCapacity / truck.maxCapacity) * 100,
      0,
    ) / plgNetwork.trucks.length,
})

const SimulationReportDocument = ({
  metrics,
  startTime,
  endTime,
  username,
  plgNetwork,
}: ReportProps) => {
  const startDate = new Date(startTime)
  const endDate = new Date(endTime)
  const durationMinutes = differenceInMinutes(endDate, startDate)
  const durationSeconds = differenceInSeconds(endDate, startDate)
  const duration = formatDuration(durationMinutes, durationSeconds)
  const globalEfficiency = (
    (metrics.fleetUtilizationPercentage +
      metrics.deliveryEfficiencyPercentage) /
    2
  ).toFixed(1)
  const systemStatus = getSystemStatus(metrics.completionPercentage)
  const networkAnalysis = plgNetwork
    ? calculateNetworkAnalysis(plgNetwork)
    : null

  return (
    <Document>
      {' '}
      <Page size="A4" style={styles.page}>
        <View style={styles.header}>
          <Image style={styles.logo} src="/logo192.png" />
          <View style={styles.headerText}>
            <Text style={styles.title}>Reporte de Simulación</Text>
            <Text style={styles.subtitle}>
              HyperLogix - Sistema de Gestión Logística
            </Text>
          </View>
        </View>{' '}
        <View style={styles.infoSection}>
          <Text style={styles.infoTitle}>Información General</Text>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Usuario:</Text>
            <Text style={styles.infoValue}>{username}</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Fecha de generación:</Text>
            <Text style={styles.infoValue}>
              {format(new Date(), "dd 'de' MMMM 'de' yyyy, HH:mm", {
                locale: es,
              })}
            </Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Inicio de simulación:</Text>
            <Text style={styles.infoValue}>
              {format(new Date(startTime), 'dd/MM/yyyy HH:mm', { locale: es })}
            </Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Fin de simulación:</Text>
            <Text style={styles.infoValue}>
              {format(new Date(endTime), 'dd/MM/yyyy HH:mm', { locale: es })}
            </Text>
          </View>{' '}
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Duración total:</Text>
            <Text style={styles.infoValue}>{duration}</Text>
          </View>
          {networkAnalysis && (
            <>
              {' '}
              <View style={styles.infoRow}>
                <Text style={styles.infoLabel}>Hora simulada inicio:</Text>
                <Text style={styles.infoValue}>
                  {networkAnalysis.totalOrders > 0 && plgNetwork
                    ? format(
                        new Date(plgNetwork.orders[0].date),
                        'dd/MM/yyyy HH:mm',
                        { locale: es },
                      )
                    : 'N/A'}
                </Text>
              </View>
              <View style={styles.infoRow}>
                <Text style={styles.infoLabel}>Hora simulada fin:</Text>
                <Text style={styles.infoValue}>
                  {networkAnalysis.completedOrders > 0 && plgNetwork
                    ? format(
                        new Date(
                          Math.max(
                            ...plgNetwork.orders
                              .filter((o) => o.status === 'COMPLETED')
                              .map((o) => new Date(o.date).getTime()),
                          ),
                        ),
                        'dd/MM/yyyy HH:mm',
                        { locale: es },
                      )
                    : 'N/A'}
                </Text>
              </View>
            </>
          )}
        </View>{' '}
        <View style={styles.metricsSection}>
          <Text style={styles.sectionTitle}>Métricas de Rendimiento</Text>
          <View style={styles.metricsGrid}>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Utilización de la flota</Text>
              <Text style={styles.metricValue}>
                {metrics.fleetUtilizationPercentage.toFixed(2)}%
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Porcentaje de finalización</Text>
              <Text style={styles.metricValue}>
                {metrics.completionPercentage.toFixed(2)}%
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>
                Utilización promedio de capacidad
              </Text>
              <Text style={styles.metricValue}>
                {metrics.averageCapacityUtilization.toFixed(2)}%
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Eficiencia de entrega</Text>
              <Text style={styles.metricValue}>
                {metrics.deliveryEfficiencyPercentage.toFixed(2)}%
              </Text>
            </View>
          </View>
        </View>{' '}
        <View style={styles.metricsSection}>
          <Text style={styles.sectionTitle}>Métricas de Tiempo</Text>
          <View style={styles.metricsGrid}>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Tiempo promedio de entrega</Text>
              <Text style={styles.metricValue}>
                {metrics.averageDeliveryTimeMinutes.toFixed(0)} min
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>
                Tiempo promedio de planificación
              </Text>
              <Text style={styles.metricValue}>
                {metrics.averagePlanificationTimeSeconds.toFixed(2)} seg
              </Text>
            </View>
          </View>
        </View>{' '}
        <View style={styles.metricsSection}>
          <Text style={styles.sectionTitle}>Distancia y Combustible</Text>
          <View style={styles.metricsGrid}>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>
                Total de distancia recorrida
              </Text>
              <Text style={styles.metricValue}>
                {metrics.totalDistanceTraveled.toFixed(2)} km
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>
                Consumo promedio de combustible
              </Text>
              <Text style={styles.metricValue}>
                {metrics.averageFuelConsumptionPerKm.toFixed(2)} L/km
              </Text>
            </View>
          </View>{' '}
        </View>{' '}
        {networkAnalysis && (
          <View style={styles.metricsSection}>
            <Text style={styles.sectionTitle}>Estadísticas de Operación</Text>
            <View style={styles.metricsGrid}>
              <View style={styles.metricCard}>
                <Text style={styles.metricTitle}>Total de pedidos</Text>
                <Text style={styles.metricValue}>
                  {networkAnalysis.totalOrders}
                </Text>
              </View>
              <View style={styles.metricCard}>
                <Text style={styles.metricTitle}>Pedidos entregados</Text>
                <Text style={styles.metricValue}>
                  {networkAnalysis.completedOrders}
                </Text>
              </View>
              <View style={styles.metricCard}>
                <Text style={styles.metricTitle}>GLP solicitado</Text>
                <Text style={styles.metricValue}>
                  {networkAnalysis.totalGLPRequested.toFixed(0)} L
                </Text>
              </View>
              <View style={styles.metricCard}>
                <Text style={styles.metricTitle}>GLP entregado</Text>
                <Text style={styles.metricValue}>
                  {networkAnalysis.totalGLPDelivered.toFixed(0)} L
                </Text>
              </View>
              <View style={styles.metricCard}>
                <Text style={styles.metricTitle}>Camiones activos</Text>
                <Text style={styles.metricValue}>
                  {networkAnalysis.activeTrucks} / {networkAnalysis.totalTrucks}
                </Text>
              </View>
              <View style={styles.metricCard}>
                <Text style={styles.metricTitle}>Combustible consumido</Text>
                <Text style={styles.metricValue}>
                  {networkAnalysis.totalFuelConsumed.toFixed(1)} L
                </Text>
              </View>
            </View>
          </View>
        )}{' '}
        <View style={styles.summarySection}>
          <Text style={styles.summaryTitle}>Resumen General</Text>
          <Text style={styles.summaryText}>
            La simulación se ejecutó durante {duration} con una eficiencia
            global del {globalEfficiency}%. El estado del sistema se califica
            como "{systemStatus}" basado en un porcentaje de finalización del{' '}
            {metrics.completionPercentage.toFixed(1)}%. La flota operó con una
            utilización del {metrics.fleetUtilizationPercentage.toFixed(1)}% y
            una eficiencia de entrega del{' '}
            {metrics.deliveryEfficiencyPercentage.toFixed(1)}%.
            {networkAnalysis &&
              networkAnalysis.totalOrders > 0 &&
              ` Se procesaron ${networkAnalysis.totalOrders} pedidos, entregando ${networkAnalysis.totalGLPDelivered.toFixed(0)}L de GLP 
              de un total solicitado de ${networkAnalysis.totalGLPRequested.toFixed(0)}L, con un consumo de combustible de ${networkAnalysis.totalFuelConsumed.toFixed(1)}L.`}
          </Text>
        </View>{' '}
        <View style={styles.footer}>
          <Text style={styles.footerText}>HyperLogix © 2025</Text>
          <Text style={styles.footerText}>Página 1 de 1</Text>
        </View>
      </Page>
    </Document>
  )
}

export const downloadSimulationReport = async (props: ReportProps) => {
  try {
    const blob = await pdf(<SimulationReportDocument {...props} />).toBlob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `reporte-simulacion-${format(new Date(), 'yyyy-MM-dd-HHmm')}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Error generating PDF:', error)
    throw error
  }
}

export default SimulationReportDocument
