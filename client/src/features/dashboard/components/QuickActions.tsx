import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Download,
  RefreshCcw,
  Route as RouteIcon,
  Terminal,
} from 'lucide-react'

export function QuickActions() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Acciones Rápidas</CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-2 gap-4">
        <Button
          variant="outline"
          className="flex flex-col h-24 items-center justify-center gap-2"
        >
          <RouteIcon className="h-6 w-6 text-blue-500" />
          <span>Optimizar Rutas</span>
        </Button>
        <Button
          variant="outline"
          className="flex flex-col h-24 items-center justify-center gap-2"
        >
          <RefreshCcw className="h-6 w-6 text-green-500" />
          <span>Sincronizar Datos</span>
        </Button>
        <Button
          variant="outline"
          className="flex flex-col h-24 items-center justify-center gap-2"
        >
          <Download className="h-6 w-6 text-purple-500" />
          <span>Respaldo</span>
        </Button>
        <Button
          variant="outline"
          className="flex flex-col h-24 items-center justify-center gap-2"
        >
          <Terminal className="h-6 w-6 text-amber-500" />
          <span>Consola</span>
        </Button>
      </CardContent>
    </Card>
  )
}
