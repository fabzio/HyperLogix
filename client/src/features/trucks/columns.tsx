import type { Truck } from '@/api'
import Typography from '@/components/typography'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { ColumnDef } from '@tanstack/react-table'

export const truckColumns: ColumnDef<Partial<Truck>>[] = [
  {
    accessorKey: 'code',
    header: 'Código',
    cell: ({ row }) => (
      <Typography variant="muted" className="font-semibold">
        {row.original.code}
      </Typography>
    ),
  },
  {
    accessorKey: 'type',
    header: 'Tipo',
    cell: ({ row }) => {
      const type = row.original.type
      const label = typeMap[type as keyof typeof typeMap]
      return (
        <Badge variant="outline" className={cn('text-xs', label)}>
          {type}
        </Badge>
      )
    },
  },
  {
    accessorKey: 'status',
    header: 'Estado',
    cell: ({ row }) => {
      const status = row.original.status
      const label = statusMap[status as keyof typeof statusMap].label
      const className = statusMap[status as keyof typeof statusMap].className
      return (
        <Badge variant="outline" className={cn('text-xs', className)}>
          {label}
        </Badge>
      )
    },
  },
  {
    accessorKey: 'tareWeight',
    header: 'Tara (t)',
    cell: ({ row }) => (
      <Typography variant="muted" className="font-semibold">
        {row.original.tareWeight?.toFixed(2)}
      </Typography>
    ),
  },
  {
    accessorKey: 'maxCapacity',
    header: 'Capacidad de GLP (m³)',
    cell: ({ row }) => (
      <Typography variant="muted" className="font-semibold">
        {row.original.maxCapacity}
      </Typography>
    ),
  },
  {
    accessorKey: 'fuelCapacity',
    header: 'Capacidad de combustible (gal)',
    cell: ({ row }) => (
      <Typography variant="muted" className="font-semibold">
        {row.original.fuelCapacity}
      </Typography>
    ),
  },
]

const statusMap = {
  ACTIVE: {
    label: 'Activo',
    className: 'bg-green-600/10 text-green-600 border-green-600/30',
  },
  MAINTENANCE: {
    label: 'Mantenimiento',
    className: 'bg-yellow-600/10 text-yellow-600 border-yellow-600/30',
  },
  BROKEN_DOWN: {
    label: 'Averiado',
    className: 'bg-red-600/10 text-red-600 border-red-600/30',
  },
  IDLE: {
    label: 'En reposo',
    className: 'bg-gray-600/10 text-gray-600 border-gray-600/30',
  },
} as const

const typeMap = {
  TA: 'bg-blue-600/10 text-blue-500 border-blue-600/30',
  TB: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  TC: 'bg-purple-600/10 text-purple-600 border-purple-600/30',
  TD: 'bg-pink-600/10 text-pink-600 border-pink-600/30',
}
