import { useSimulationStore } from '../../store/simulation'

export default function Metrics() {
  const { metrics } = useSimulationStore()
  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold">Métricas de Simulación</h2>
      {metrics ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <strong>Utilización de la flota:</strong>{' '}
            {metrics.fleetUtilizationPercentage.toFixed(2)}%
          </div>
          <div>
            <strong>Consumo promedio de combustible por km:</strong>{' '}
            {metrics.averageFuelConsumptionPerKm.toFixed(2)} L/km
          </div>
          <div>
            <strong>Porcentaje de finalización:</strong>{' '}
            {metrics.completionPercentage.toFixed(2)}%
          </div>
          <div>
            <strong>Tiempo promedio de entrega:</strong>{' '}
            {metrics.averageDeliveryTimeMinutes.toFixed(2)} minutos
          </div>
          <div>
            <strong>Utilización promedio de capacidad:</strong>{' '}
            {metrics.averageCapacityUtilization.toFixed(2)}%
          </div>
          <div>
            <strong>Tiempo promedio de planificación:</strong>{' '}
            {metrics.averagePlanificationTimeSeconds.toFixed(2)} segundos
          </div>
          <div>
            <strong>Total de distancia recorrida:</strong>{' '}
            {metrics.totalDistanceTraveled.toFixed(2)} km
          </div>
          <div>
            <strong>Eficiencia de entrega:</strong>{' '}
            {metrics.deliveryEfficiencyPercentage.toFixed(2)}%
          </div>
        </div>
      ) : (
        <p>No hay métricas disponibles.</p>
      )}
    </div>
  )
}
