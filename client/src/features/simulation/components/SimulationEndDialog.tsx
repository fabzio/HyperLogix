import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useSessionStore } from '@/store/session'
import { Download, FileText } from 'lucide-react'
import { useState } from 'react'
import { useSimulationStore } from '../store/simulation'
import { downloadSimulationReport } from './SimulationReportPDF'

interface Props {
  open: boolean
  onClose: () => void
  reason: 'completed' | 'manual' | null
}

export default function SimulationEndDialog({ open, onClose, reason }: Props) {
  const {
    finalMetrics,
    simulationStartTime,
    simulationEndTime,
    finalPlgNetwork,
  } = useSimulationStore()
  const { username } = useSessionStore()
  const [isGenerating, setIsGenerating] = useState(false)

  const handleDownloadReport = async () => {
    if (
      !finalMetrics ||
      !simulationStartTime ||
      !simulationEndTime ||
      !username
    ) {
      console.error('Missing data for report generation')
      return
    }

    setIsGenerating(true)
    try {
      await downloadSimulationReport({
        metrics: finalMetrics,
        startTime: simulationStartTime,
        endTime: simulationEndTime,
        username,
        plgNetwork: finalPlgNetwork || undefined,
      })
    } catch (error) {
      console.error('Error generating report:', error)
    } finally {
      setIsGenerating(false)
    }
  }

  const canGenerateReport =
    finalMetrics && simulationStartTime && simulationEndTime && username

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Simulación finalizada
          </DialogTitle>
          <DialogDescription>
            {reason === 'manual' ? (
              <p>Visualizador finalizado manualmente con éxito</p>
            ) : (
              <p>
                Visualizador finalizado porque todos los pedidos fueron
                completados.
              </p>
            )}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-3 mt-4">
          {canGenerateReport && (
            <Button
              onClick={handleDownloadReport}
              disabled={isGenerating}
              className="w-full"
              variant="outline"
            >
              <Download className="h-4 w-4 mr-2" />
              {isGenerating ? 'Generando reporte...' : 'Descargar reporte PDF'}
            </Button>
          )}

          <Button onClick={onClose} className="w-full">
            Cerrar
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
