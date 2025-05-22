import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { resourcesData } from '../data/mock-data'

export function ResourceAllocation() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Asignación de Recursos</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {resourcesData.map((resource) => (
          <div key={resource.name}>
            <div className="flex justify-between mb-1">
              <span className="text-sm font-medium">{resource.name}</span>
              <span
                className={`text-sm font-medium text-${resource.color}-500`}
              >
                {resource.value}% asignado
              </span>
            </div>
            <Progress value={resource.value} className="h-2" />
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
