import { SemaphoreIndicator } from '@/components/SemaphoreIndicator'
import Typography from '@/components/typography'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useSimulationStore } from '@/features/simulation/store/simulation'
import { useNavigate, useSearch } from '@tanstack/react-router'
import StationDetail from './StationDetail'

export default function Stations() {
  const { stationId } = useSearch({ from: '/_auth/simulacion' })
  const { plgNetwork, simulationTime } = useSimulationStore()
  const navigate = useNavigate({ from: '/simulacion' })
  const stations = plgNetwork?.stations || []

  if (stationId) {
    return <StationDetail stationId={stationId} />
  }

  return (
    <article>
      <Typography variant="h3">Estaciones</Typography>
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
              <TableRow
                key={station.id}
                className="hover:bg-muted cursor-pointer"
                onClick={() => {
                  navigate({
                    to: '/simulacion',
                    search: { stationId: station.id },
                  })
                }}
              >
                <TableCell>{station.name}</TableCell>
                <TableCell>
                  ({station.location.x}, {station.location.y})
                </TableCell>
                <TableCell>
                  {station.mainStation ? (
                    '♾️'
                  ) : (
                    <div className="flex items-center gap-2">
                      <SemaphoreIndicator
                        value={
                          (simulationTime &&
                            station.availableCapacityPerDate[
                              simulationTime.split('T')[0]
                            ]) ||
                          station.maxCapacity
                        }
                        maxValue={station.maxCapacity}
                        showAsIndicator={true}
                        thresholds={{
                          excellent: 80,
                          good: 60,
                          warning: 40,
                          danger: 20,
                        }}
                      />
                      <span>
                        {(simulationTime &&
                          station.availableCapacityPerDate[
                            simulationTime.split('T')[0]
                          ]?.toFixed(2)) ||
                          station.maxCapacity}{' '}
                        / {station.maxCapacity} m³
                      </span>
                    </div>
                  )}
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
    </article>
  )
}
