import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { Truck } from '@/domain/Truck'
import { TruckState } from '@/domain/TruckState'
import { cn } from '@/lib/utils'
import {
  AlertTriangle,
  Fuel,
  MapPin,
  MoreHorizontal,
  RotateCcw,
  Settings,
  Truck as TruckIcon,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import {
  useReportTruckBreakdown,
  useReportTruckMaintenance,
  useRestoreTruckToIdle,
} from '../hooks/useOperationMutations'

interface TrucksListProps {
  trucks: Truck[]
}

const typeColorMap = {
  TA: 'bg-blue-600/10 text-blue-500 border-blue-600/30',
  TB: 'bg-orange-600/10 text-orange-600 border-orange-600/30',
  TC: 'bg-purple-600/10 text-purple-600 border-purple-600/30',
  TD: 'bg-pink-600/10 text-pink-600 border-pink-600/30',
}

const statusColorMap = {
  [TruckState.ACTIVE]: 'bg-green-600/10 text-green-600 border-green-600/30',
  [TruckState.IDLE]: 'bg-gray-600/10 text-gray-600 border-gray-600/30',
  [TruckState.MAINTENANCE]:
    'bg-yellow-600/10 text-yellow-600 border-yellow-600/30',
  [TruckState.BROKEN_DOWN]: 'bg-red-600/10 text-red-600 border-red-600/30',
}

const statusLabels = {
  [TruckState.ACTIVE]: 'Activo',
  [TruckState.IDLE]: 'Inactivo',
  [TruckState.MAINTENANCE]: 'Mantenimiento',
  [TruckState.BROKEN_DOWN]: 'Averiado',
}

const generatePaginationNumbers = (currentPage: number, totalPages: number) => {
  const surroundingPageCount = 1
  const finalPageNumbers: (number | string)[] = []
  let previousPage: number | undefined

  for (let pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
    const isPageAtStartOrEnd = pageNumber === 1 || pageNumber === totalPages
    const isPageWithinSurrounding =
      Math.abs(currentPage - pageNumber) <= surroundingPageCount

    if (isPageAtStartOrEnd || isPageWithinSurrounding) {
      if (previousPage && pageNumber - previousPage > 1) {
        finalPageNumbers.push('...')
      }
      finalPageNumbers.push(pageNumber)
      previousPage = pageNumber
    }
  }

  return finalPageNumbers
}

export default function TrucksList({ trucks }: TrucksListProps) {
  const [page, setPage] = useState(1)
  const [pageSize] = useState(8)

  const { mutate: reportBreakdown, isPending: isReportingBreakdown } =
    useReportTruckBreakdown()
  const { mutate: reportMaintenance, isPending: isReportingMaintenance } =
    useReportTruckMaintenance()
  const { mutate: restoreToIdle, isPending: isRestoringToIdle } =
    useRestoreTruckToIdle()

  const handleReportBreakdown = (truckId: string) => {
    reportBreakdown(
      { truckId, request: { reason: 'Avería reportada manualmente' } },
      {
        onSuccess: () => {
          toast.success('Avería reportada exitosamente')
        },
        onError: (error) => {
          toast.error(`Error al reportar avería: ${error.message}`)
        },
      },
    )
  }

  const handleReportMaintenance = (truckId: string) => {
    reportMaintenance(
      { truckId, request: { reason: 'Mantenimiento programado' } },
      {
        onSuccess: () => {
          toast.success('Mantenimiento programado exitosamente')
        },
        onError: (error) => {
          toast.error(`Error al programar mantenimiento: ${error.message}`)
        },
      },
    )
  }

  const handleRestoreToIdle = (truckId: string) => {
    restoreToIdle(
      { truckId },
      {
        onSuccess: () => {
          toast.success('Camión restaurado a estado inactivo')
        },
        onError: (error) => {
          toast.error(`Error al restaurar camión: ${error.message}`)
        },
      },
    )
  }

  // Sort trucks by capacity utilization (lowest first)
  const sortedTrucks = trucks.sort(
    (t1, t2) =>
      t1.currentCapacity / t1.maxCapacity - t2.currentCapacity / t2.maxCapacity,
  )

  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedTrucks = sortedTrucks.slice(startIndex, endIndex)
  const totalPages = Math.ceil(trucks.length / pageSize)

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setPage(newPage)
    }
  }

  const paginationNumbers = generatePaginationNumbers(page, totalPages)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <TruckIcon className="h-5 w-5" />
          Flota de Camiones
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex justify-between items-center text-sm text-muted-foreground">
            <span>Total: {trucks.length} camiones</span>
            <span>
              Página {page} de {totalPages}
            </span>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Código
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Estado
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Combustible
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Capacidad
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Ubicación
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Acciones
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {paginatedTrucks.length > 0 ? (
                paginatedTrucks.map((truck) => (
                  <TableRow key={truck.id} className="hover:bg-muted/50">
                    <TableCell className="px-2 py-2">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm">
                          {truck.code}
                        </span>
                        {truck.type && (
                          <Badge
                            variant="outline"
                            className={cn(
                              'text-xs px-1 py-0 h-5',
                              typeColorMap[
                                truck.type as keyof typeof typeColorMap
                              ],
                            )}
                          >
                            {truck.type}
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="px-2 py-2">
                      <Badge
                        variant="outline"
                        className={cn(
                          'text-xs px-2 py-1',
                          statusColorMap[truck.status],
                        )}
                      >
                        {statusLabels[truck.status]}
                      </Badge>
                    </TableCell>
                    <TableCell className="px-2 py-2">
                      <div className="flex items-center gap-1">
                        <Fuel className="h-3 w-3 text-blue-500" />
                        <span className="text-xs">
                          {Math.floor(truck.currentFuel)}/
                          {Math.floor(truck.fuelCapacity)}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="px-2 py-2">
                      <div className="flex items-center gap-1">
                        <div className="w-12 h-2 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className={cn(
                              'h-full rounded-full transition-all',
                              truck.currentCapacity / truck.maxCapacity > 0.7
                                ? 'bg-red-500'
                                : truck.currentCapacity / truck.maxCapacity >
                                    0.4
                                  ? 'bg-yellow-500'
                                  : 'bg-green-500',
                            )}
                            style={{
                              width: `${(truck.currentCapacity / truck.maxCapacity) * 100}%`,
                            }}
                          />
                        </div>
                        <span className="text-xs">
                          {Math.floor(truck.currentCapacity)}/
                          {truck.maxCapacity}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="px-2 py-2">
                      <div className="flex items-center gap-1">
                        <MapPin className="h-3 w-3 text-muted-foreground" />
                        <span className="text-xs">
                          {Math.floor(truck.location.x)},
                          {Math.floor(truck.location.y)}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="px-2 py-2">
                      <div className="flex items-center gap-1">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0"
                            >
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem
                              onClick={() =>
                                handleReportBreakdown(truck.id || '')
                              }
                              disabled={truck.status === TruckState.BROKEN_DOWN}
                            >
                              <AlertTriangle className="mr-2 h-4 w-4" />
                              Reportar Avería
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() =>
                                handleReportMaintenance(truck.id || '')
                              }
                              disabled={truck.status === TruckState.MAINTENANCE}
                            >
                              <Settings className="mr-2 h-4 w-4" />
                              Programar Mantenimiento
                            </DropdownMenuItem>
                            {(truck.status === TruckState.BROKEN_DOWN ||
                              truck.status === TruckState.MAINTENANCE) && (
                              <DropdownMenuItem
                                onClick={() =>
                                  handleRestoreToIdle(truck.id || '')
                                }
                              >
                                <RotateCcw className="mr-2 h-4 w-4" />
                                Restaurar a Inactivo
                              </DropdownMenuItem>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell
                    colSpan={6}
                    className="text-center px-2 py-4 text-sm text-muted-foreground"
                  >
                    No hay camiones disponibles
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>

          {totalPages > 1 && (
            <Pagination>
              <PaginationContent>
                <PaginationItem>
                  <PaginationPrevious
                    onClick={() => handlePageChange(page - 1)}
                    aria-disabled={page === 1}
                    className={cn(
                      'cursor-pointer',
                      page === 1 ? 'opacity-50 pointer-events-none' : '',
                    )}
                  />
                </PaginationItem>
                {paginationNumbers.map((pageNum, idx) =>
                  pageNum === '...' ? (
                    <PaginationItem
                      key={`ellipsis-${page}-${paginationNumbers[idx - 1] || 0}-${paginationNumbers[idx + 1] || 0}`}
                    >
                      <span className="px-2">...</span>
                    </PaginationItem>
                  ) : (
                    <PaginationLink
                      key={pageNum}
                      onClick={() => handlePageChange(Number(pageNum))}
                      className={cn(
                        'cursor-pointer',
                        page === pageNum
                          ? 'bg-primary text-primary-foreground'
                          : '',
                      )}
                    >
                      {pageNum}
                    </PaginationLink>
                  ),
                )}
                <PaginationItem>
                  <PaginationNext
                    onClick={() => handlePageChange(page + 1)}
                    aria-disabled={page === totalPages}
                    className={cn(
                      'cursor-pointer',
                      page === totalPages
                        ? 'opacity-50 pointer-events-none'
                        : '',
                    )}
                  />
                </PaginationItem>
              </PaginationContent>
            </Pagination>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
