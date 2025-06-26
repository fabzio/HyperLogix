import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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
import type { Order } from '@/domain/Order'
import type { Routes } from '@/domain/Routes'
import { cn } from '@/lib/utils'
import { Clock, Package, User } from 'lucide-react'
import { useState } from 'react'

interface OrdersListProps {
  orders: Order[]
  simulationTime?: string
  routes?: Routes
}

type OrderStatus = 'PENDING' | 'CALCULATING' | 'IN_PROGRESS' | 'COMPLETED'

const statusColorMap: Record<OrderStatus, string> = {
  PENDING: 'bg-yellow-600/10 text-yellow-600 border-yellow-600/30',
  IN_PROGRESS: 'bg-blue-600/10 text-blue-600 border-blue-600/30',
  CALCULATING: 'bg-purple-600/10 text-purple-600 border-purple-600/30',
  COMPLETED: 'bg-green-600/10 text-green-600 border-green-600/30',
}

const statusLabels: Record<OrderStatus, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En Progreso',
  CALCULATING: 'Calculando',
  COMPLETED: 'Completado',
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
      if (previousPage && pageNumber - pageNumber > 1) {
        finalPageNumbers.push('...')
      }
      finalPageNumbers.push(pageNumber)
      previousPage = pageNumber
    }
  }

  return finalPageNumbers
}

const formatRemainingTime = (
  maxDeliveryDate: string,
  simulationTime?: string,
) => {
  if (!simulationTime) return '--'

  const maxDate = new Date(maxDeliveryDate)
  const currentDate = new Date(simulationTime)
  const remainingTime = Math.max(
    Math.ceil((maxDate.getTime() - currentDate.getTime()) / (1000 * 60 * 60)),
    0,
  )

  if (remainingTime === 0) return 'Vencido'
  if (remainingTime < 24) return `${remainingTime}h`
  return `${Math.ceil(remainingTime / 24)}d`
}

const getEstimatedArrivalTime = (
  orderId: string,
  routes?: Routes,
): string | null => {
  if (!routes?.stops) return null

  // Find the order in any truck's route
  for (const stops of Object.values(routes.stops)) {
    const orderStop = stops.find(
      (stop) => stop.node?.id === orderId && stop.node?.type === 'DELIVERY',
    )

    if (orderStop?.arrivalTime) {
      return orderStop.arrivalTime
    }
  }

  return null
}

const formatEstimatedArrival = (
  arrivalTime: string | null,
  simulationTime?: string,
) => {
  if (!arrivalTime || !simulationTime) return '--'

  const arrival = new Date(arrivalTime)
  const current = new Date(simulationTime)
  const diff = arrival.getTime() - current.getTime()

  // If arrival is in the past, return "En progreso"
  if (diff <= 0) return 'En progreso'

  const hours = Math.ceil(diff / (1000 * 60 * 60))

  if (hours < 1) return '< 1h'
  if (hours < 24) return `~${hours}h`

  const days = Math.ceil(hours / 24)
  return `~${days}d`
}

export default function OrdersList({
  orders,
  simulationTime,
  routes,
}: OrdersListProps) {
  const [page, setPage] = useState(1)
  const [pageSize] = useState(8)

  // Sort orders by priority (pending first, then by remaining time)
  const sortedOrders = orders.sort((a, b) => {
    // Pending orders first
    if (a.status === 'PENDING' && b.status !== 'PENDING') return -1
    if (b.status === 'PENDING' && a.status !== 'PENDING') return 1

    // Then by remaining time (earliest first)
    if (simulationTime) {
      const aRemaining =
        new Date(a.maxDeliveryDate).getTime() -
        new Date(simulationTime).getTime()
      const bRemaining =
        new Date(b.maxDeliveryDate).getTime() -
        new Date(simulationTime).getTime()
      return aRemaining - bRemaining
    }

    return 0
  })

  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedOrders = sortedOrders.slice(startIndex, endIndex)
  const totalPages = Math.ceil(orders.length / pageSize)

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setPage(newPage)
    }
  }

  const paginationNumbers = generatePaginationNumbers(page, totalPages)

  // Calculate summary stats
  const pendingCount = orders.filter((o) => o.status === 'PENDING').length
  const inProgressCount = orders.filter(
    (o) => o.status === 'IN_PROGRESS',
  ).length
  const completedCount = orders.filter((o) => o.status === 'COMPLETED').length

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Package className="h-5 w-5" />
          Pedidos
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Summary stats */}
          <div className="grid grid-cols-3 gap-2 text-sm">
            <div className="text-center p-2 bg-yellow-50 rounded">
              <div className="font-semibold text-yellow-600">
                {pendingCount}
              </div>
              <div className="text-xs text-yellow-600">Pendientes</div>
            </div>
            <div className="text-center p-2 bg-blue-50 rounded">
              <div className="font-semibold text-blue-600">
                {inProgressCount}
              </div>
              <div className="text-xs text-blue-600">En Progreso</div>
            </div>
            <div className="text-center p-2 bg-green-50 rounded">
              <div className="font-semibold text-green-600">
                {completedCount}
              </div>
              <div className="text-xs text-green-600">Completados</div>
            </div>
          </div>

          <div className="flex justify-between items-center text-sm text-muted-foreground">
            <span>Total: {orders.length} pedidos</span>
            <span>
              Página {page} de {totalPages}
            </span>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  ID
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Cliente
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Estado
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  GLP
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Llegada Est.
                </TableHead>
                <TableHead className="px-2 py-2 text-left font-semibold text-xs">
                  Tiempo Límite
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {paginatedOrders.length > 0 ? (
                paginatedOrders.map((order) => {
                  const remainingTime = formatRemainingTime(
                    order.maxDeliveryDate,
                    simulationTime,
                  )
                  const isUrgent =
                    simulationTime &&
                    new Date(order.maxDeliveryDate).getTime() -
                      new Date(simulationTime).getTime() <
                      24 * 60 * 60 * 1000

                  const estimatedArrival = getEstimatedArrivalTime(
                    order.id,
                    routes,
                  )
                  const formattedEstimatedArrival = formatEstimatedArrival(
                    estimatedArrival,
                    simulationTime,
                  )

                  return (
                    <TableRow key={order.id} className="hover:bg-muted/50">
                      <TableCell className="px-2 py-2">
                        <span className="font-medium text-sm">{order.id}</span>
                      </TableCell>
                      <TableCell className="px-2 py-2">
                        <div className="flex items-center gap-1">
                          <User className="h-3 w-3 text-muted-foreground" />
                          <span className="text-xs truncate max-w-20">
                            {order.clientId}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="px-2 py-2">
                        <Badge
                          variant="outline"
                          className={cn(
                            'text-xs px-2 py-1',
                            statusColorMap[order.status],
                          )}
                        >
                          {statusLabels[order.status]}
                        </Badge>
                      </TableCell>
                      <TableCell className="px-2 py-2">
                        <div className="flex items-center gap-1">
                          <div className="w-12 h-2 bg-gray-200 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-blue-500 rounded-full transition-all"
                              style={{
                                width: `${(order.deliveredGLP / order.requestedGLP) * 100}%`,
                              }}
                            />
                          </div>
                          <span className="text-xs">
                            {order.deliveredGLP}/{order.requestedGLP}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="px-2 py-2">
                        <div className="flex items-center gap-1">
                          <Clock className="h-3 w-3 text-muted-foreground" />
                          <span className="text-xs">
                            {formattedEstimatedArrival}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="px-2 py-2">
                        <div className="flex items-center gap-1">
                          <Clock
                            className={cn(
                              'h-3 w-3',
                              isUrgent
                                ? 'text-red-500'
                                : 'text-muted-foreground',
                            )}
                          />
                          <span
                            className={cn(
                              'text-xs',
                              isUrgent ? 'text-red-600 font-medium' : '',
                            )}
                          >
                            {remainingTime}
                          </span>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })
              ) : (
                <TableRow>
                  <TableCell
                    colSpan={6}
                    className="text-center px-2 py-4 text-sm text-muted-foreground"
                  >
                    No hay pedidos disponibles
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
