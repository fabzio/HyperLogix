import { useSimulationStore } from '../../store/simulation'

export default function Planification() {
  const { planificationStatus } = useSimulationStore()
  return (
    <div>
      {planificationStatus ? (
        <div>
          <h3>{planificationStatus.planning}</h3>
          <p>Nodos procesados: {planificationStatus.currentNodesProcessed}</p>
        </div>
      ) : (
        <p>No hay planificación en curso.</p>
      )}
    </div>
  )
}
