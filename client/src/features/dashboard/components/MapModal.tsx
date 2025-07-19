import { Button } from '@/components/ui/button'
import { useNavigate } from '@tanstack/react-router'
import { MapPin } from 'lucide-react'

interface MapModalProps {
  asQuickAction?: boolean
}

export function MapModal({ asQuickAction = false }: MapModalProps) {
  const navigate = useNavigate({ from: '/' })

  const handleNavigateToMap = () => {
    navigate({ to: '/map' })
  }

  return asQuickAction ? (
    <Button
      variant="outline"
      onClick={handleNavigateToMap}
      className="flex flex-col h-20 items-center justify-center gap-2"
    >
      <MapPin className="h-5 w-5 text-cyan-500" />
      <span className="text-xs">Ver Mapa</span>
    </Button>
  ) : (
    <Button variant="outline" onClick={handleNavigateToMap}>
      <MapPin className="h-4 w-4 mr-2" />
      Ver Mapa
    </Button>
  )
}
