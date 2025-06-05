import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface Props {
  open: boolean
  onClose: () => void
  reason: 'completed' | 'manual' | null
}

export default function SimulationEndDialog({ open, onClose, reason }: Props) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Simulación finalizada</DialogTitle>
          <DialogDescription>
            {reason === 'manual' ? (
              <p>Simulación detenida manualmente con éxito</p>
            ) : (
              <p>Simulación finalizada porque todos los pedidos fueron completados.</p>
            )}
          </DialogDescription>
        </DialogHeader>
        <div className="flex justify-end mt-4">
          <Button onClick={onClose}>Cerrar</Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}