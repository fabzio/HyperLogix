import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AlertCircle } from 'lucide-react'
import { alertsData } from '../data/mock-data'

export function SystemAlerts() {
  return (
    <Card className="border-l-4 border-amber-500">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm flex items-center gap-2">
          <AlertCircle className="h-4 w-4 text-amber-500" />
          Alertas del Sistema
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3 text-sm">
          {alertsData.map((alert) => {
            const colorClass = alert.type === 'error' ? 'red' : 'amber'

            return (
              <div key={alert.message} className="flex items-center gap-2">
                <span className={`w-2 h-2 bg-${colorClass}-500 rounded-full`} />
                <span>{alert.message}</span>
              </div>
            )
          })}
        </div>
      </CardContent>
    </Card>
  )
}
