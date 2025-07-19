import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import type { Station } from '@/domain/Station'
import { useDeleteStation } from '@/features/stations/hooks/useStationMutations'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { useState } from 'react'

interface DeleteStationDialogProps {
  station: Station
  children?: React.ReactNode
}

export default function DeleteStationDialog({
  station,
  children,
}: DeleteStationDialogProps) {
  const [open, setOpen] = useState(false)
  const { mutate: deleteStation, isPending } = useDeleteStation()

  const handleDelete = () => {
    deleteStation(station.id, {
      onSuccess: () => {
        setOpen(false)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {children || (
          <Button variant="outline" size="sm">
            <Trash2 className="h-4 w-4 mr-2" />
            Eliminar
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-red-500" />
            Confirmar Eliminación
          </DialogTitle>
          <DialogDescription>
            Esta acción no se puede deshacer. La estación será eliminada
            permanentemente.
          </DialogDescription>
        </DialogHeader>
        <div className="rounded-lg border p-4 bg-muted">
          <p className="text-sm font-medium">Estación a eliminar:</p>
          <p className="text-sm text-muted-foreground mt-1">{station.name}</p>
          <p className="text-sm text-muted-foreground">
            Capacidad: {station.maxCapacity} m³
          </p>
          <p className="text-sm text-muted-foreground">
            Ubicación: ({station.location?.x || 0}, {station.location?.y || 0})
          </p>
          {station.mainStation && (
            <p className="text-sm text-muted-foreground">
              Tipo: Estación Principal
            </p>
          )}
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => setOpen(false)}
            disabled={isPending}
          >
            Cancelar
          </Button>
          <Button
            variant="destructive"
            onClick={handleDelete}
            disabled={isPending}
          >
            {isPending ? 'Eliminando...' : 'Eliminar Estación'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
