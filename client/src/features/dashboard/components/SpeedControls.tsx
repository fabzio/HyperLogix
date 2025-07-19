import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { FastForward, Gauge, Pause, Play, Rewind, Timer } from 'lucide-react'
import { toast } from 'sonner'
import {
  useOperationCommand,
  useOperationStatus,
} from '../hooks/useOperationMutations'
import { useOperationStore } from '../store/operation'

export function SpeedControls() {
  const { isConnected, planificationStatus, simulationTime } =
    useOperationStore()

  const { data: operationStatus } = useOperationStatus()

  const { mutate: sendCommand, isPending } = useOperationCommand()

  const handlePause = async () => {
    if (!isConnected) {
      toast.error('Sistema desconectado.')
      return
    }

    try {
      await sendCommand('PAUSE')
      toast.success('Operación pausada.')
    } catch (error) {
      toast.error('Error al pausar la operación.')
      console.error('Error pausing operation:', error)
    }
  }

  const handleResume = async () => {
    if (!isConnected) {
      toast.error('Sistema desconectado.')
      return
    }

    try {
      await sendCommand('RESUME')
      toast.success('Operación reanudada.')
    } catch (error) {
      toast.error('Error al reanudar la operación.')
      console.error('Error resuming operation:', error)
    }
  }

  const handleAccelerate = async () => {
    if (!isConnected) {
      toast.error('Sistema desconectado.')
      return
    }

    try {
      await sendCommand('ACCELERATE')
      toast.success('Velocidad aumentada.')
    } catch (error) {
      toast.error('Error al acelerar la operación.')
      console.error('Error accelerating operation:', error)
    }
  }

  const handleDecelerate = async () => {
    if (!isConnected) {
      toast.error('Sistema desconectado.')
      return
    }

    try {
      await sendCommand('DESACCELERATE')
      toast.success('Velocidad disminuida.')
    } catch (error) {
      toast.error('Error al desacelerar la operación.')
      console.error('Error decelerating operation:', error)
    }
  }

  const currentTime = simulationTime
    ? new Date(simulationTime).toLocaleString('es-ES')
    : 'Tiempo no disponible'

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Timer className="h-5 w-5" />
          Control de Velocidad
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Current time display */}
          <div className="text-center">
            <div className="text-sm text-muted-foreground">
              Tiempo de Planificación
            </div>
            <div className="text-lg font-semibold">{currentTime}</div>
          </div>

          {/* Acceleration display */}
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 mb-2">
              <Gauge className="h-4 w-4 text-blue-500" />
              <span className="text-sm text-muted-foreground">Aceleración</span>
            </div>
            <Badge variant="secondary" className="text-lg font-bold">
              {operationStatus?.timeAcceleration?.toFixed(1) || '1.0'}x
            </Badge>
          </div>

          {/* Speed controls */}
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleDecelerate}
              disabled={!isConnected || isPending}
              className="flex items-center gap-2"
            >
              <Rewind className="h-4 w-4" />
              Desacelerar
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleAccelerate}
              disabled={!isConnected || isPending}
              className="flex items-center gap-2"
            >
              <FastForward className="h-4 w-4" />
              Acelerar
            </Button>
          </div>

          {/* Pause/Resume controls */}
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handlePause}
              disabled={!isConnected || isPending}
              className="flex items-center gap-2"
            >
              <Pause className="h-4 w-4" />
              Pausar
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleResume}
              disabled={!isConnected || isPending}
              className="flex items-center gap-2"
            >
              <Play className="h-4 w-4" />
              Reanudar
            </Button>
          </div>

          {/* Status indicators */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Estado:</span>
              <Badge variant={isConnected ? 'default' : 'destructive'}>
                {isConnected ? 'Conectado' : 'Desconectado'}
              </Badge>
            </div>

            {planificationStatus?.planning && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">
                  Planificando:
                </span>
                <Badge variant="secondary">En progreso</Badge>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
