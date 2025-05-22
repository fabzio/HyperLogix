import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { performanceData } from '../data/mock-data'

export function PerformanceChart() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Rendimiento del Sistema</CardTitle>
        <Tabs defaultValue="vehiculos" className="w-full">
          <TabsList className="grid w-full max-w-md grid-cols-3">
            <TabsTrigger value="vehiculos">Vehículos</TabsTrigger>
            <TabsTrigger value="entregas">Entregas</TabsTrigger>
            <TabsTrigger value="rutas">Rutas</TabsTrigger>
          </TabsList>
          <TabsContent value="vehiculos" className="mt-4">
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={performanceData}>
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="vehiculos"
                  stroke="#3b82f6"
                  fill="#3b82f680"
                />
              </AreaChart>
            </ResponsiveContainer>
          </TabsContent>
          <TabsContent value="entregas" className="mt-4">
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={performanceData}>
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="entregas"
                  stroke="#8b5cf6"
                  fill="#8b5cf680"
                />
              </AreaChart>
            </ResponsiveContainer>
          </TabsContent>
          <TabsContent value="rutas" className="mt-4">
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={performanceData}>
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="rutas"
                  stroke="#06b6d4"
                  fill="#06b6d480"
                />
              </AreaChart>
            </ResponsiveContainer>
          </TabsContent>
        </Tabs>
      </CardHeader>
    </Card>
  )
}
