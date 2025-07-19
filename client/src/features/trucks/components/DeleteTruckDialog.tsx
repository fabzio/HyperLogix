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
import type { Truck } from '@/domain/Truck'
import { useDeleteTruck } from '@/features/trucks/hooks/useTruckMutations'
import { Trash2 } from 'lucide-react'
import { useState } from 'react'

interface DeleteTruckDialogProps {
  truck: Truck
}

export default function DeleteTruckDialog({ truck }: DeleteTruckDialogProps) {
  const [open, setOpen] = useState(false)
  const { mutate: deleteTruck, isPending } = useDeleteTruck()

  const handleDelete = () => {
    deleteTruck(truck.id, {
      onSuccess: () => {
        setOpen(false)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <Trash2 className="h-4 w-4 mr-2" />
          Eliminar
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>¿Está seguro?</DialogTitle>
          <DialogDescription>
            Esta acción no se puede deshacer. El camión{' '}
            <strong>{truck.code}</strong> será eliminado permanentemente del
            sistema.
          </DialogDescription>
        </DialogHeader>
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
            {isPending ? 'Eliminando...' : 'Eliminar'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
