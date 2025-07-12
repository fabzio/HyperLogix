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
    metrics, // Add current metrics as fallback
  } = useSimulationStore()
  const { username } = useSessionStore()
  const [isGenerating, setIsGenerating] = useState(false)

  const handleDownloadReport = async () => {
    // Use finalMetrics if available, otherwise use current metrics
    const metricsToUse = finalMetrics || metrics

    if (!metricsToUse || !simulationStartTime || !username) {
      console.error('Missing data for report generation')
      return
    }

    setIsGenerating(true)
    try {
      await downloadSimulationReport({
        metrics: metricsToUse,
        startTime: simulationStartTime,
        endTime: simulationEndTime || new Date().toISOString(), // Use current time if no end time
        username,
        plgNetwork: finalPlgNetwork || undefined,
      })
    } catch (error) {
      console.error('Error generating report:', error)
    } finally {
      setIsGenerating(false)
    }
  }

  // Update condition to only require metrics, start time, and username
  const canGenerateReport =
    (finalMetrics || metrics) && simulationStartTime && username

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Simulación finalizada
          </DialogTitle>
          <DialogDescription>
            {reason === 'manual'
              ? 'Visualizador finalizado manualmente con éxito'
              : 'Visualizador finalizado porque todos los pedidos fueron completados.'}
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
