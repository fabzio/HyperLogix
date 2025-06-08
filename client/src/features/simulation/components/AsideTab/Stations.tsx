import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useSimulationStore } from '../../store/simulation'

export default function Stations() {
  const { plgNetwork, simulationTime } = useSimulationStore()
  const stations = plgNetwork?.stations || []
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Nombre de Estación</TableHead>
          <TableHead>Ubicación</TableHead>
          <TableHead>Capacidad de Combustible</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {stations.length > 0 ? (
          stations.map((station) => (
            <TableRow key={station.id} className="hover:bg-muted">
              <TableCell>{station.name}</TableCell>
              <TableCell>
                ({station.location.x}, {station.location.y})
              </TableCell>
              <TableCell>
                {(simulationTime &&
                  station.availableCapacityPerDate[simulationTime]?.toFixed(
                    2,
                  )) ||
                station.mainStation
                  ? '♾️'
                  : station.maxCapacity}{' '}
                m³
              </TableCell>
            </TableRow>
          ))
        ) : (
          <TableRow>
            <TableCell colSpan={3} className="text-center px-4 py-2">
              No hay estaciones disponibles
            </TableCell>
          </TableRow>
        )}
      </TableBody>
    </Table>
  )
}
