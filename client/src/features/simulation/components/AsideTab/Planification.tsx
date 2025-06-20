import Typography from '@/components/typography'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Activity, Brain } from 'lucide-react'
import { useSimulationStore } from '../../store/simulation'

export default function Planification() {
  const { planificationStatus } = useSimulationStore()

  return (
    <article className="h-full flex flex-col">
      <Typography variant="h3" className="mb-4">
        Planificación
      </Typography>

      {planificationStatus ? (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm">
              <Brain className="h-4 w-4 text-blue-500" />
              Estado de Planificación
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0">
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Activity className="h-3 w-3 text-green-500" />
                <span className="text-sm font-medium">
                  {planificationStatus.planning}
                </span>
              </div>

              <div className="bg-muted rounded-md p-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-muted-foreground">
                    Nodos procesados
                  </span>
                  <span className="text-lg font-bold text-foreground">
                    {planificationStatus.currentNodesProcessed}
                  </span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card className="flex-1">
          <CardContent className="pt-6 h-full flex items-center justify-center">
            <div className="text-center space-y-2">
              <Brain className="h-8 w-8 text-muted-foreground mx-auto" />
              <p className="text-sm text-muted-foreground">
                No hay planificación en curso
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </article>
  )
}
