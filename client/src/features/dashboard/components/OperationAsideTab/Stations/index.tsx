import Typography from '@/components/typography'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useWatchOperation } from '../../../hooks/useOperation'

export default function Stations() {
  const { stationId } = useSearch({ from: '/_auth/map' })
  const { plgNetwork } = useWatchOperation()
  const navigate = useNavigate({ from: '/map' })
  const stations = plgNetwork?.stations || []

  // Si hay una estación seleccionada, mostrar detalles
  if (stationId) {
    const selectedStation = stations.find((s) => s.id === stationId)
    return (
      <div className="h-full flex flex-col">
        <div className="p-4 border-b">
          <Typography variant="h3">Detalle de Estación</Typography>
          <button
            type="button"
            onClick={() => navigate({ to: '/map', search: {} })}
            className="text-sm text-blue-600 hover:underline"
          >
            ← Volver a la lista
          </button>
        </div>
        <div className="p-4">
          {selectedStation ? (
            <div className="space-y-4">
              <div>
                <div className="text-sm font-medium">Nombre</div>
                <p className="text-sm text-muted-foreground">
                  {selectedStation.name}
                </p>
              </div>
              <div>
                <div className="text-sm font-medium">ID</div>
                <p className="text-sm text-muted-foreground">
                  {selectedStation.id}
                </p>
              </div>
              <div>
                <div className="text-sm font-medium">Ubicación</div>
                <p className="text-sm text-muted-foreground">
                  X: {selectedStation.location.x.toFixed(4)}, Y:{' '}
                  {selectedStation.location.y.toFixed(4)}
                </p>
              </div>
              <div>
                <div className="text-sm font-medium">Capacidad Máxima</div>
                <p className="text-sm text-muted-foreground">
                  {selectedStation.maxCapacity} L
                </p>
              </div>
              <div>
                <div className="text-sm font-medium">Estación Principal</div>
                <p className="text-sm text-muted-foreground">
                  {selectedStation.mainStation ? 'Sí' : 'No'}
                </p>
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              Estación no encontrada
            </p>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b">
        <Typography variant="h3">Estaciones</Typography>
        <p className="text-sm text-muted-foreground">
          {stations.length} estaciones disponibles
        </p>
      </div>

      <div className="flex-1 overflow-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nombre</TableHead>
              <TableHead>Ubicación</TableHead>
              <TableHead>Capacidad</TableHead>
              <TableHead>Tipo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {stations.length > 0 ? (
              stations.map((station) => (
                <TableRow
                  key={station.id}
                  className={`cursor-pointer hover:bg-muted ${
                    stationId === station.id ? 'bg-muted' : ''
                  }`}
                  onClick={() => {
                    navigate({
                      to: '/map',
                      search: { stationId: station.id },
                    })
                  }}
                >
                  <TableCell className="font-medium">{station.name}</TableCell>
                  <TableCell>
                    <div className="text-sm">
                      X: {station.location.x.toFixed(4)}, Y:{' '}
                      {station.location.y.toFixed(4)}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{station.maxCapacity} L</div>
                  </TableCell>
                  <TableCell>
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${
                        station.mainStation
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {station.mainStation ? 'Principal' : 'Secundaria'}
                    </span>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={4}
                  className="text-center text-muted-foreground"
                >
                  No hay estaciones disponibles
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
