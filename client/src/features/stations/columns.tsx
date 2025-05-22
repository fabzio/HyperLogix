import Typography from '@/components/typography'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import type { Station } from '@/domain/Station'
import type { ColumnDef } from '@tanstack/react-table'

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
        {row.original.mainStation ? 'Abastecido' : row.original.maxCapacity}
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
]
