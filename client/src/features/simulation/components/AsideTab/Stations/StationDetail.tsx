import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Separator } from '@/components/ui/separator'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useSimulationStore } from '@/features/simulation/store/simulation'
import { useNavigate } from '@tanstack/react-router'
import {
  ArrowLeft,
  Calendar,
  Clock,
  Droplets,
  FuelIcon,
  MapPin,
  Package,
  Truck,
} from 'lucide-react'
import { useState } from 'react'

interface Props {
  stationId: string
}

export default function StationDetail({ stationId }: Props) {
  const { plgNetwork, simulationTime } = useSimulationStore()
  const navigate = useNavigate({ from: '/simulacion' })
  const [showOnlyToday, setShowOnlyToday] = useState(false)

  const station = plgNetwork?.stations.find((s) => s.id === stationId)

  if (!station) {
    return <div>Estación no encontrada</div>
  }

  const currentDate = simulationTime?.split('T')[0] || ''
  const availableCapacity =
    station.availableCapacityPerDate[currentDate] || station.maxCapacity

  // Filter reservations based on checkbox
  const filteredReservations = showOnlyToday
    ? station.reservationHistory.filter((reservation) => {
        const reservationDate = new Date(reservation.dateTime)
          .toISOString()
          .split('T')[0]
        return reservationDate === currentDate
      })
    : station.reservationHistory

  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() =>
            navigate({
              to: '/simulacion',
              search: {},
            })
          }
          className="flex items-center gap-1 text-sm w-fit p-0 h-auto underline hover:no-underline"
        >
          <ArrowLeft className="h-3 w-3" />
          Volver
        </Button>

        <CardTitle className="flex items-center gap-2">
          <FuelIcon className="h-5 w-5" />
          {station.name}
          {station.mainStation && (
            <Badge
              variant="outline"
              className="text-xs bg-blue-600/10 text-blue-600 border-blue-600/30"
            >
              Principal
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="space-y-4">
          {/* Station Details Section */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-blue-500" />
              <span className="text-sm font-medium">Ubicación</span>
            </div>
            <div className="ml-6">
              <span className="text-sm">
                ({station.location.x}, {station.location.y})
              </span>
            </div>
          </div>

          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Droplets className="h-4 w-4 text-orange-500" />
              <span className="text-sm font-medium">Capacidad</span>
            </div>
            <div className="ml-6">
              {station.mainStation ? (
                <div className="flex items-center gap-2">
                  <span className="text-2xl">♾️</span>
                  <span className="text-sm">Capacidad ilimitada</span>
                </div>
              ) : (
                <div className="space-y-2">
                  <span className="text-sm">
                    {availableCapacity.toFixed(2)} / {station.maxCapacity} m³
                  </span>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-blue-500 h-2 rounded-full transition-all"
                      style={{
                        width: `${(availableCapacity / station.maxCapacity) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              )}
            </div>
          </div>

          <Separator />

          {/* Reservation History Section */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-green-500" />
                <span className="text-sm font-medium">
                  Historial de Reservas
                </span>
              </div>
              {station.reservationHistory.length > 0 && (
                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="show-only-today"
                    checked={showOnlyToday}
                    onCheckedChange={(checked) =>
                      setShowOnlyToday(checked === true)
                    }
                  />
                  <label
                    htmlFor="show-only-today"
                    className="text-xs text-muted-foreground cursor-pointer"
                  >
                    Solo hoy
                  </label>
                </div>
              )}
            </div>
            <div className="ml-6">
              {filteredReservations.length > 0 ? (
                <div className="max-h-[300px] overflow-y-auto border rounded-md">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="px-2 py-1 text-left font-semibold text-xs">
                          Fecha/Hora
                        </TableHead>
                        <TableHead className="px-2 py-1 text-left font-semibold text-xs">
                          Cantidad
                        </TableHead>
                        <TableHead className="px-2 py-1 text-left font-semibold text-xs">
                          Camión
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {filteredReservations
                        .sort(
                          (a, b) =>
                            new Date(b.dateTime).getTime() -
                            new Date(a.dateTime).getTime(),
                        )
                        .map((reservation) => {
                          const truck = plgNetwork?.trucks.find(
                            (t) => t.id === reservation.vehicleId,
                          )
                          const order = plgNetwork?.orders.find(
                            (o) => o.id === reservation.orderId,
                          )

                          return (
                            <TableRow
                              key={`${reservation.dateTime}-${reservation.vehicleId}`}
                              className="hover:bg-muted/50"
                            >
                              <TableCell className="px-2 py-1">
                                <div className="flex flex-col gap-1">
                                  <div className="flex items-center gap-1 text-xs">
                                    <Calendar className="h-3 w-3" />
                                    {new Date(
                                      reservation.dateTime,
                                    ).toLocaleDateString()}
                                  </div>
                                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                                    <Clock className="h-3 w-3" />
                                    {new Date(
                                      reservation.dateTime,
                                    ).toLocaleTimeString([], {
                                      hour: '2-digit',
                                      minute: '2-digit',
                                    })}
                                  </div>
                                </div>
                              </TableCell>
                              <TableCell className="px-2 py-1">
                                <div className="flex items-center gap-1">
                                  <Droplets className="h-3 w-3 text-orange-500" />
                                  <span className="text-xs font-medium">
                                    {reservation.amount.toFixed(2)} m³
                                  </span>
                                </div>
                              </TableCell>
                              <TableCell className="px-2 py-1">
                                <button
                                  type="button"
                                  className="flex items-center gap-1 cursor-pointer hover:text-blue-500 bg-transparent border-none p-0 text-left"
                                  onClick={() => {
                                    navigate({
                                      to: '/simulacion',
                                      search: {
                                        truckId: +reservation.vehicleId,
                                      },
                                    })
                                  }}
                                >
                                  <Truck className="h-3 w-3" />
                                  <span className="text-xs">
                                    {truck?.code ||
                                      `Camión ${reservation.vehicleId}`}
                                  </span>
                                </button>
                              </TableCell>
                            </TableRow>
                          )
                        })}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                <div className="text-sm text-muted-foreground">
                  No hay historial de reservas disponible
                </div>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
