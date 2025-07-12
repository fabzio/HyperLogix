import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { Truck, X } from 'lucide-react'

interface TruckSelectionBannerProps {
  truckCode: string
  truckType: string
  onDeselect: () => void
  className?: string
}

const truckTypeColors = {
  TA: 'bg-purple-100 text-purple-800 border-purple-200 dark:bg-purple-900/20 dark:text-purple-400 dark:border-purple-800/50',
  TB: 'bg-sky-100 text-sky-800 border-sky-200 dark:bg-sky-900/20 dark:text-sky-400 dark:border-sky-800/50',
  TC: 'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800/50',
  TD: 'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-400 dark:border-yellow-800/50',
}

export function TruckSelectionBanner({
  truckCode,
  truckType,
  onDeselect,
  className,
}: TruckSelectionBannerProps) {
  return (
    <div
      className={cn(
        'flex items-center justify-center gap-2 bg-background/95 backdrop-blur-sm border-b px-4 py-2 shadow-sm',
        className,
      )}
    >
      <div className="flex items-center gap-2 text-sm">
        <Truck className="h-3 w-3" />
        <span className="font-medium">Camión:</span>
        <span className="font-mono font-bold">{truckCode}</span>
        <Badge
          variant="outline"
          className={cn(
            'text-xs h-5 px-1.5',
            truckTypeColors[truckType as keyof typeof truckTypeColors] ||
              truckTypeColors.TA,
          )}
        >
          {truckType}
        </Badge>
      </div>
      <Button
        variant="ghost"
        size="icon"
        onClick={onDeselect}
        className="h-5 w-5 hover:bg-muted"
        title="Deseleccionar camión"
      >
        <X className="h-3 w-3" />
      </Button>
    </div>
  )
}
