import Typography from '@/components/typography'
import { Checkbox } from '@/components/ui/checkbox'
import type { Station } from '@/domain/Station'
import type { ColumnDef } from '@tanstack/react-table'
import DeleteStationDialog from './components/DeleteStationDialog'
import EditStationDialog from './components/EditStationDialog'

export const stationColumns: ColumnDef<Partial<Station>>[] = [
  {
    accessorKey: 'name',
    header: 'Nombre',
  },
  {
    accessorKey: 'maxCapacity',
    header: 'Capacidad Máxima (m³)',
    cell: ({ row }) => (
      <Typography variant="small" className="font-semibold">
        {row.original.mainStation ? '∞ (Infinita)' : row.original.maxCapacity}
      </Typography>
    ),
  },
  {
    accessorKey: 'location',
    header: 'Ubicación',
    cell: (info) => {
      const loc = info.row.original.location
      return loc ? `${loc.x}, ${loc.y}` : ''
    },
  },
  {
    accessorKey: 'mainStation',
    header: 'Principal',
    cell: ({ row }) => (
      <div className="ml-5">
        <Checkbox checked={row.original.mainStation} disabled />
      </div>
    ),
  },
  {
    id: 'actions',
    enableHiding: false,
    cell: ({ row }) => {
      const station = row.original as Station

      // Solo mostrar acciones si la station tiene ID
      if (!station.id) {
        return null
      }

      return (
        <div className="flex gap-2">
          <EditStationDialog station={station} />
          <DeleteStationDialog station={station} />
        </div>
      )
    },
  },
]
