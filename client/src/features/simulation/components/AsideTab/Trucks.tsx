import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useSimulationStore } from '../../store/simulation'

export default function Trucks() {
  const { plgNetwork } = useSimulationStore()
  const trucks = plgNetwork?.trucks || []
  return (
    <div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="px-4 py-2 text-left font-semibold">
              ID
            </TableHead>
            <TableHead className="px-4 py-2 text-left font-semibold">
              Combustible
            </TableHead>
            <TableHead className="px-4 py-2 text-left font-semibold">
              Capacidad
            </TableHead>
            <TableHead className="px-4 py-2 text-left font-semibold">
              Ubicación
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {trucks.length > 0 ? (
            trucks.map((truck) => (
              <TableRow key={truck.code} className="hover:bg-muted">
                <TableCell className="px-4 py-2">{truck.id}</TableCell>
                <TableCell className="px-4 py-2">
                  {truck.currentFuel.toFixed(2)} / {truck.fuelCapacity}
                </TableCell>
                <TableCell className="px-4 py-2">
                  {truck.currentCapacity} / {truck.maxCapacity}
                </TableCell>
                <TableCell className="px-4 py-2">
                  {truck.location.x.toFixed(4)}, {truck.location.y.toFixed(4)}
                </TableCell>
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell colSpan={3} className="text-center px-4 py-2">
                No hay camiones disponibles
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  )
}
