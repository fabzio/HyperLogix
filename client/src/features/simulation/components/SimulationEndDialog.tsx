import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useSessionStore } from '@/store/session'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import { Download, FileText } from 'lucide-react'
import { useState } from 'react'
import { useSimulationStore } from '../store/simulation'
import { downloadSimulationReport } from './SimulationReportPDF'

interface Props {
  open: boolean
  onClose: () => void
  reason: 'completed' | 'manual' | 'collapse' | null
}

export default function SimulationEndDialog({ open, onClose, reason }: Props) {
  const {
    finalMetrics,
    simulationStartTime,
    simulationEndTime,
    finalPlgNetwork,
    metrics, // Add current metrics as fallback
    collapseInfo,
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

  const getDialogContent = () => {
    switch (reason) {
      case 'collapse': {
        // Agregar logs para depurar el problema de la fecha en el diálogo
        console.log('=== DEBUG DIÁLOGO COLAPSO ===')
        console.log('collapseInfo completo:', collapseInfo)
        console.log('collapseInfo.timestamp:', collapseInfo?.timestamp)

        const collapseTimestamp = collapseInfo?.timestamp
          ? format(new Date(collapseInfo.timestamp), 'PPpp', {
              locale: es,
            })
          : 'Fecha no disponible'

        console.log('Timestamp formateado que se mostrará:', collapseTimestamp)

        return {
          title: 'Simulación detenida por colapso',
          description:
            'No hay camiones disponibles para completar las entregas.',
          icon: '⚠️',
          timestamp: collapseTimestamp,
        }
      }
      case 'manual':
        return {
          title: 'Simulación finalizada',
          description: 'Visualizador finalizado manualmente con éxito',
          icon: '📋',
        }
      case 'completed':
        return {
          title: 'Simulación completada',
          description:
            'Visualizador finalizado porque todos los pedidos fueron completados.',
          icon: '✅',
        }
      default:
        return {
          title: 'Simulación finalizada',
          description: 'La simulación ha terminado.',
          icon: '📋',
        }
    }
  }

  const dialogContent = getDialogContent()

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <span className="text-xl">{dialogContent.icon}</span>
            {dialogContent.title}
          </DialogTitle>
          <DialogDescription>{dialogContent.description}</DialogDescription>
          {dialogContent.timestamp && (
            <div className="mt-2 p-3  border border-red-200 rounded-md">
              <p className="text-sm text-800 font-medium">
                📅 Fecha y hora del colapso:
              </p>
              <p className="text-sm text-700">{dialogContent.timestamp}</p>
            </div>
          )}
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
