import {
  Document,
  Image,
  Page,
  StyleSheet,
  Text,
  View,
  pdf,
} from '@react-pdf/renderer'
import { differenceInMinutes, format } from 'date-fns'
import { es } from 'date-fns/locale'

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

interface ReportData {
  metrics: SimulationMetrics
  startTime: string
  endTime: string
  username: string
}

const styles = StyleSheet.create({
  page: {
    flexDirection: 'column',
    backgroundColor: '#ffffff',
    padding: 40,
    fontFamily: 'Helvetica',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 30,
    paddingBottom: 20,
    borderBottomWidth: 2,
    borderBottomColor: '#e5e7eb',
  },
  logo: {
    width: 50,
    height: 50,
  },
  headerText: {
    flex: 1,
    marginLeft: 20,
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
  headerInfo: {
    alignItems: 'flex-end',
  },
  infoText: {
    fontSize: 10,
    color: '#6b7280',
    marginBottom: 2,
  },
  section: {
    marginBottom: 25,
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
    backgroundColor: '#f9fafb',
    padding: 15,
    marginBottom: 15,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  metricTitle: {
    fontSize: 11,
    color: '#6b7280',
    marginBottom: 5,
    fontWeight: 'bold',
  },
  metricValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1f2937',
  },
  metricUnit: {
    fontSize: 10,
    color: '#6b7280',
    marginTop: 2,
  },
  summaryCard: {
    backgroundColor: '#f0f9ff',
    padding: 20,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#bfdbfe',
  },
  summaryTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#1e40af',
    marginBottom: 10,
  },
  summaryGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  summaryItem: {
    alignItems: 'center',
  },
  summaryLabel: {
    fontSize: 10,
    color: '#6b7280',
    marginBottom: 3,
  },
  summaryValue: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#1e40af',
  },
  footer: {
    position: 'absolute',
    bottom: 40,
    left: 40,
    right: 40,
    textAlign: 'center',
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    paddingTop: 10,
  },
  footerText: {
    fontSize: 8,
    color: '#6b7280',
  },
})

const SimulationReportPDF = ({ data }: { data: ReportData }) => {
  const duration = differenceInMinutes(
    new Date(data.endTime),
    new Date(data.startTime),
  )

  const globalEfficiency = (
    (data.metrics.fleetUtilizationPercentage +
      data.metrics.deliveryEfficiencyPercentage) /
    2
  ).toFixed(1)

  const systemStatus =
    data.metrics.completionPercentage > 80
      ? 'Óptimo'
      : data.metrics.completionPercentage > 60
        ? 'Bueno'
        : 'Mejorable'

  return (
    <Document>
      <Page size="A4" style={styles.page}>
        {/* Header */}
        <View style={styles.header}>
          <Image style={styles.logo} src="/logo192.png" />
          <View style={styles.headerText}>
            <Text style={styles.title}>Reporte de Simulación</Text>
            <Text style={styles.subtitle}>
              HyperLogix - Sistema de Gestión Logística
            </Text>
          </View>
          <View style={styles.headerInfo}>
            <Text style={styles.infoText}>
              Fecha: {format(new Date(), 'dd/MM/yyyy HH:mm', { locale: es })}
            </Text>
            <Text style={styles.infoText}>Usuario: {data.username}</Text>
            <Text style={styles.infoText}>
              Duración: {Math.floor(duration / 60)}h {duration % 60}m
            </Text>
          </View>
        </View>

        {/* Simulation Details */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Detalles de la Simulación</Text>
          <View style={styles.metricsGrid}>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Inicio de Simulación</Text>
              <Text style={styles.metricValue}>
                {format(new Date(data.startTime), 'dd/MM/yyyy', { locale: es })}
              </Text>
              <Text style={styles.metricUnit}>
                {format(new Date(data.startTime), 'HH:mm', { locale: es })}
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Fin de Simulación</Text>
              <Text style={styles.metricValue}>
                {format(new Date(data.endTime), 'dd/MM/yyyy', { locale: es })}
              </Text>
              <Text style={styles.metricUnit}>
                {format(new Date(data.endTime), 'HH:mm', { locale: es })}
              </Text>
            </View>
          </View>
        </View>

        {/* Performance Metrics */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Métricas de Rendimiento</Text>
          <View style={styles.metricsGrid}>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Utilización de la Flota</Text>
              <Text style={styles.metricValue}>
                {data.metrics.fleetUtilizationPercentage.toFixed(2)}%
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Porcentaje de Finalización</Text>
              <Text style={styles.metricValue}>
                {data.metrics.completionPercentage.toFixed(2)}%
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Utilización de Capacidad</Text>
              <Text style={styles.metricValue}>
                {data.metrics.averageCapacityUtilization.toFixed(2)}%
              </Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Eficiencia de Entrega</Text>
              <Text style={styles.metricValue}>
                {data.metrics.deliveryEfficiencyPercentage.toFixed(2)}%
              </Text>
            </View>
          </View>
        </View>

        {/* Time & Distance Metrics */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>
            Métricas de Tiempo y Distancia
          </Text>
          <View style={styles.metricsGrid}>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Tiempo Promedio de Entrega</Text>
              <Text style={styles.metricValue}>
                {data.metrics.averageDeliveryTimeMinutes.toFixed(0)}
              </Text>
              <Text style={styles.metricUnit}>minutos</Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Tiempo de Planificación</Text>
              <Text style={styles.metricValue}>
                {data.metrics.averagePlanificationTimeSeconds.toFixed(2)}
              </Text>
              <Text style={styles.metricUnit}>segundos</Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Distancia Total Recorrida</Text>
              <Text style={styles.metricValue}>
                {data.metrics.totalDistanceTraveled.toFixed(2)}
              </Text>
              <Text style={styles.metricUnit}>kilómetros</Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricTitle}>Consumo de Combustible</Text>
              <Text style={styles.metricValue}>
                {data.metrics.averageFuelConsumptionPerKm.toFixed(2)}
              </Text>
              <Text style={styles.metricUnit}>L/km</Text>
            </View>
          </View>
        </View>

        {/* Summary */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Resumen Ejecutivo</Text>
          <View style={styles.summaryCard}>
            <Text style={styles.summaryTitle}>
              Análisis General del Sistema
            </Text>
            <View style={styles.summaryGrid}>
              <View style={styles.summaryItem}>
                <Text style={styles.summaryLabel}>Eficiencia Global</Text>
                <Text style={styles.summaryValue}>{globalEfficiency}%</Text>
              </View>
              <View style={styles.summaryItem}>
                <Text style={styles.summaryLabel}>Estado del Sistema</Text>
                <Text style={styles.summaryValue}>{systemStatus}</Text>
              </View>
              <View style={styles.summaryItem}>
                <Text style={styles.summaryLabel}>Pedidos Completados</Text>
                <Text style={styles.summaryValue}>
                  {data.metrics.completionPercentage.toFixed(0)}%
                </Text>
              </View>
            </View>
          </View>
        </View>

        {/* Footer */}
        <View style={styles.footer}>
          <Text style={styles.footerText}>
            Este reporte fue generado automáticamente por el sistema HyperLogix
          </Text>
          <Text style={styles.footerText}>
            © 2025 HyperLogix. Todos los derechos reservados.
          </Text>
        </View>
      </Page>
    </Document>
  )
}

export const generateSimulationReport = async (data: ReportData) => {
  const doc = <SimulationReportPDF data={data} />
  const pdfBlob = await pdf(doc).toBlob()

  // Create download link
  const url = URL.createObjectURL(pdfBlob)
  const link = document.createElement('a')
  link.href = url
  link.download = `reporte-simulacion-${format(new Date(), 'yyyy-MM-dd-HHmm')}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export type { ReportData, SimulationMetrics }
