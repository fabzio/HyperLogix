import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import type { Truck } from '@/domain/Truck'
import { cn } from '@/lib/utils'
import { useNavigate, useSearch } from '@tanstack/react-router'
import {
  ArrowLeft,
  Droplets,
  Fuel,
  MapPin,
  Truck as TruckIcon,
  Wrench,
} from 'lucide-react'
import { useWatchOperation } from '../../../hooks/useOperation'

const typeMap = {
  TA: 'bg-blue-600/10 text-blue-500 border-blue-600/30',
  TB: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  TC: 'bg-purple-600/10 text-purple-600 border-purple-600/30',
  TD: 'bg-pink-600/10 text-pink-600 border-pink-600/30',
}

const statusMap = {
  AVAILABLE: 'bg-green-600/10 text-green-600 border-green-600/30',
  IN_TRANSIT: 'bg-blue-600/10 text-blue-600 border-blue-600/30',
  DELIVERING: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  LOADING: 'bg-yellow-600/10 text-yellow-600 border-yellow-600/30',
  MAINTENANCE: 'bg-red-600/10 text-red-600 border-red-600/30',
  OUT_OF_SERVICE: 'bg-gray-600/10 text-gray-600 border-gray-600/30',
}

const statusText = {
  AVAILABLE: 'Disponible',
  IN_TRANSIT: 'En Tránsito',
  DELIVERING: 'Entregando',
  LOADING: 'Cargando',
  MAINTENANCE: 'Mantenimiento',
  OUT_OF_SERVICE: 'Fuera de Servicio',
}

export default function TruckDetail() {
  const { truckId } = useSearch({ from: '/_auth/map' })
  const { plgNetwork } = useWatchOperation()
  const navigate = useNavigate({ from: '/map' })

  const truck = plgNetwork?.trucks?.find((t: Truck) => t.id === String(truckId))

  if (!truck) {
    return (
      <Card className="w-full">
        <CardContent className="p-6">
          <div className="text-center text-muted-foreground">
            Camión no encontrado
          </div>
        </CardContent>
      </Card>
    )
  }

  // Calculate maintenance status
  const nextMaintenanceDate = new Date(truck.nextMaintenance)
  const currentDate = new Date()
  const daysUntilMaintenance = Math.ceil(
    (nextMaintenanceDate.getTime() - currentDate.getTime()) /
      (1000 * 60 * 60 * 24),
  )

  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() =>
            navigate({
              to: '/map',
              search: { truckId: undefined },
            })
          }
          className="flex items-center gap-1 text-sm w-fit p-0 h-auto underline hover:no-underline"
        >
          <ArrowLeft className="h-3 w-3" />
          Volver
        </Button>

        <CardTitle className="flex items-center gap-2">
          <TruckIcon className="h-5 w-5" />
          {truck.code}
          <Badge
            variant="outline"
            className={cn(
              'text-xs',
              typeMap[truck.type as keyof typeof typeMap] ||
                'bg-gray-600/10 text-gray-600 border-gray-600/30',
            )}
          >
            {truck.type}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="space-y-4">
          {/* Status Section */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">Estado:</span>
              <Badge
                variant="outline"
                className={cn(
                  'text-xs',
                  statusMap[truck.status as keyof typeof statusMap] ||
                    'bg-gray-600/10 text-gray-600 border-gray-600/30',
                )}
              >
                {statusText[truck.status as keyof typeof statusText] ||
                  truck.status}
              </Badge>
            </div>
          </div>

          <Separator />

          {/* Fuel Level */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Fuel className="h-4 w-4 text-blue-500" />
                <span className="text-sm font-medium">Combustible</span>
              </div>
              <span className="text-xs text-muted-foreground">
                {truck.currentFuel.toFixed(1)}gal / {truck.fuelCapacity}gal
              </span>
            </div>
            <div className="flex gap-1">
              {Array.from({ length: 10 }, (_, i) => {
                const threshold = (truck.fuelCapacity / 10) * (i + 1)
                const isFilled = truck.currentFuel >= threshold
                const fuelPercentage = truck.currentFuel / truck.fuelCapacity
                return (
                  <div
                    key={`fuel-bar-${truck.id}-${i}`}
                    className={`h-3 flex-1 rounded-sm border ${
                      isFilled
                        ? fuelPercentage > 0.3
                          ? 'bg-green-500 border-green-600'
                          : fuelPercentage > 0.1
                            ? 'bg-yellow-500 border-yellow-600'
                            : 'bg-red-500 border-red-600'
                        : 'bg-gray-200 border-gray-300'
                    }`}
                  />
                )
              })}
            </div>
          </div>

          {/* Capacity Tank */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Droplets className="h-4 w-4 text-orange-500" />
                <span className="text-sm font-medium">Capacidad</span>
              </div>
              <span className="text-xs text-muted-foreground">
                {truck.currentCapacity.toFixed(1)}m³ / {truck.maxCapacity}m³
              </span>
            </div>
            <div className="relative">
              <Progress
                value={(truck.currentCapacity / truck.maxCapacity) * 100}
                className="h-8"
              />
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-xs font-medium text-white drop-shadow-sm">
                  {Math.round(
                    (truck.currentCapacity / truck.maxCapacity) * 100,
                  )}
                  %
                </span>
              </div>
            </div>
          </div>

          <Separator />

          {/* Vehicle Specifications */}
          <div className="space-y-3">
            <span className="text-sm font-medium">Especificaciones</span>
            <div className="ml-4 space-y-2 text-sm text-muted-foreground">
              <div className="flex justify-between">
                <span>Peso vacío:</span>
                <span>{truck.tareWeight.toFixed(1)}kg</span>
              </div>
              <div className="flex justify-between">
                <span>Capacidad máxima:</span>
                <span>{truck.maxCapacity.toFixed(1)}m³</span>
              </div>
              <div className="flex justify-between">
                <span>Tanque combustible:</span>
                <span>{truck.fuelCapacity}gal</span>
              </div>
            </div>
          </div>

          <Separator />

          {/* Maintenance Section */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Wrench className="h-4 w-4 text-gray-500" />
              <span className="text-sm font-medium">Mantenimiento</span>
            </div>
            <div className="ml-6">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">
                  Próximo mantenimiento:
                </span>
                <div className="text-right">
                  <div className="text-sm">
                    {nextMaintenanceDate.toLocaleDateString()}
                  </div>
                  <div
                    className={cn(
                      'text-xs',
                      daysUntilMaintenance <= 7
                        ? 'text-red-500'
                        : daysUntilMaintenance <= 14
                          ? 'text-yellow-500'
                          : 'text-green-500',
                    )}
                  >
                    {daysUntilMaintenance > 0
                      ? `${daysUntilMaintenance} días`
                      : daysUntilMaintenance === 0
                        ? 'Hoy'
                        : `${Math.abs(daysUntilMaintenance)} días atrasado`}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <Separator />

          {/* Location Section */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-green-500" />
              <span className="text-sm font-medium">Ubicación</span>
            </div>
            <div className="ml-6">
              <div className="text-sm text-muted-foreground">
                <div>X: {truck.location.x.toFixed(6)}</div>
                <div>Y: {truck.location.y.toFixed(6)}</div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2 pt-4">
            <Button
              variant="outline"
              size="sm"
              className="flex-1"
              onClick={() => {
                // Navigate back to map
                navigate({
                  to: '/map',
                  search: { truckId: undefined },
                })
              }}
            >
              <MapPin className="h-3 w-3 mr-1" />
              Ver en Mapa
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
