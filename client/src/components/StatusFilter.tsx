import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

interface StatusFilterProps {
  value: string
  onChange: (value: string) => void
  className?: string
}

const statusOptions = [
  { value: 'all', label: 'Todos' },
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'CALCULATING', label: 'Calculando' },
  { value: 'IN_PROGRESS', label: 'En Progreso' },
  { value: 'COMPLETED', label: 'Completado' },
]

const statusColors = {
  all: 'bg-gray-100 text-gray-800 border-gray-200 dark:bg-gray-900/20 dark:text-gray-400 dark:border-gray-800/50',
  PENDING:
    'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-400 dark:border-yellow-800/50',
  CALCULATING:
    'bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-900/20 dark:text-blue-400 dark:border-blue-800/50',
  IN_PROGRESS:
    'bg-orange-100 text-orange-800 border-orange-200 dark:bg-orange-900/20 dark:text-orange-400 dark:border-orange-800/50',
  COMPLETED:
    'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800/50',
}

export function StatusFilter({
  value,
  onChange,
  className,
}: StatusFilterProps) {
  return (
    <div className={cn('flex flex-wrap gap-2', className)}>
      {statusOptions.map((option) => (
        <Button
          key={option.value}
          variant={value === option.value ? 'default' : 'outline'}
          size="sm"
          onClick={() => onChange(option.value)}
          className={cn(
            'h-7 px-3 text-xs',
            value === option.value
              ? statusColors[option.value as keyof typeof statusColors]
              : 'hover:bg-muted',
          )}
        >
          {option.label}
        </Button>
      ))}
    </div>
  )
}
