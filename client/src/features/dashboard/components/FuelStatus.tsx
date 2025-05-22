import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Fuel } from 'lucide-react'
import { fuelData } from '../data/mock-data'

export function FuelStatus() {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm flex items-center gap-2">
          <Fuel className="h-4 w-4 text-blue-500" />
          Estado de Combustible
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {fuelData.map((item) => (
            <div key={item.name}>
              <div className="flex justify-between text-xs mb-1">
                <span>{item.name}</span>
                <span>{item.details || `${item.value}%`}</span>
              </div>
              <Progress value={item.value} className="h-2" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
