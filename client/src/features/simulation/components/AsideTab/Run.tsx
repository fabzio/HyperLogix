import { Button } from '@/components/ui/button'
import { useStartSimulation } from '../../hooks/useSimulation'

export default function Run() {
  const { refetch } = useStartSimulation()
  return (
    <div>
      <Button onClick={() => refetch()} className="w-full">
        Ejecutar
      </Button>
    </div>
  )
}
