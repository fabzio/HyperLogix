import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Shield } from 'lucide-react'
import { systemStatusData } from '../data/mock-data'

export function SystemStatus() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Shield className="h-5 w-5 text-green-500" />
          Estado del Sistema
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {systemStatusData.map((item) => {
            const colorClass = item.statusType === 'success' ? 'green' : 'amber'

            return (
              <div
                key={item.name}
                className="flex justify-between items-center"
              >
                <div className="flex items-center gap-2">
                  <span
                    className={`w-2 h-2 bg-${colorClass}-500 rounded-full`}
                  />
                  <span>{item.name}</span>
                </div>
                <Badge
                  variant="outline"
                  className={`bg-${colorClass}-500/10 text-${colorClass}-500 border-${colorClass}-500/20`}
                >
                  {item.status}
                </Badge>
              </div>
            )
          })}
        </div>
      </CardContent>
    </Card>
  )
}
