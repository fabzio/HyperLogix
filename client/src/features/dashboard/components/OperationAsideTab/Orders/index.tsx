import { SearchFilter } from '@/components/SearchFilter'
import { StatusFilter } from '@/components/StatusFilter'
import Typography from '@/components/typography'
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
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useState } from 'react'
import { useWatchOperation } from '../../../hooks/useOperation'
import OrderDetail from './OrderDetail'

export default function Orders() {
  const { truckId, orderId } = useSearch({ from: '/_auth/map' })
  const { plgNetwork, routes, simulationTime } = useWatchOperation()
  const navigate = useNavigate({ from: '/map' })
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [searchFilter, setSearchFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')

  const allOrders =
    plgNetwork?.orders.filter((order) => {
      if (!truckId) return true
      const stops = routes?.stops?.[truckId.toString()] || []
      return stops.some((stop) => stop.node.id === order.id)
    }) || []

  const filteredOrders = allOrders.filter((order) => {
    // Filter by search text
    const matchesSearch =
      order.id.toLowerCase().includes(searchFilter.toLowerCase()) ||
      order.clientId?.toLowerCase().includes(searchFilter.toLowerCase())

    // Filter by status
    const matchesStatus =
      statusFilter === 'all' || order.status === statusFilter

    // Filter by simulation time (only show orders that should be visible)
    const matchesTime =
      !simulationTime || new Date(order.date) <= new Date(simulationTime)

    return matchesSearch && matchesStatus && matchesTime
  })

  const orders = filteredOrders.sort((a, b) => {
    const dateA = new Date(a.date)
    const dateB = new Date(b.date)
    return dateA.getTime() - dateB.getTime()
  })

  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedOrders = orders.slice(startIndex, endIndex)
  const totalPages = Math.ceil(orders.length / pageSize)

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-500'
      case 'IN_PROGRESS':
        return 'bg-blue-500'
      case 'COMPLETED':
        return 'bg-green-500'
      case 'CANCELLED':
        return 'bg-red-500'
      default:
        return 'bg-gray-500'
    }
  }

  const handleOrderClick = (clickedOrderId: string) => {
    if (orderId === clickedOrderId) {
      navigate({ search: { ...(truckId && { truckId }) } })
    } else {
      navigate({
        search: {
          orderId: clickedOrderId,
          ...(truckId && { truckId }),
        },
      })
    }
  }

  if (orderId) {
    const selectedOrder = allOrders.find((o) => o.id === orderId)
    if (selectedOrder) {
      return <OrderDetail />
    }
  }

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b">
        <Typography variant="h3">
          {truckId ? `Órdenes del Camión ${truckId}` : 'Todas las Órdenes'}
        </Typography>
        <p className="text-sm text-muted-foreground">
          {orders.length} órdenes {truckId ? 'asignadas' : 'activas'}
        </p>
      </div>

      <div className="p-4 space-y-3">
        <SearchFilter
          value={searchFilter}
          onChange={setSearchFilter}
          placeholder="Buscar orden o cliente..."
        />
        <StatusFilter value={statusFilter} onChange={setStatusFilter} />
      </div>

      <div className="flex-1 overflow-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Cliente</TableHead>
              <TableHead>Cantidad</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead>Fecha</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {paginatedOrders.map((order) => (
              <TableRow
                key={order.id}
                className={`cursor-pointer hover:bg-muted ${
                  orderId === order.id ? 'bg-muted' : ''
                }`}
                onClick={() => handleOrderClick(order.id)}
              >
                <TableCell className="font-medium">{order.id}</TableCell>
                <TableCell>{order.clientId || 'N/A'}</TableCell>
                <TableCell>{order.requestedGLP} L</TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <div
                      className={`w-2 h-2 rounded-full ${getStatusColor(order.status)}`}
                    />
                    <span className="text-sm">{order.status}</span>
                  </div>
                </TableCell>
                <TableCell className="text-sm">
                  {new Date(order.date).toLocaleDateString('es-ES', {
                    day: '2-digit',
                    month: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="p-4 border-t">
          <Pagination>
            <PaginationContent>
              {page > 1 && (
                <PaginationItem>
                  <PaginationPrevious
                    onClick={() => setPage(page - 1)}
                    className="cursor-pointer"
                  />
                </PaginationItem>
              )}
              {Array.from({ length: totalPages }, (_, i) => i + 1).map(
                (pageNum) => (
                  <PaginationItem key={pageNum}>
                    <PaginationLink
                      onClick={() => setPage(pageNum)}
                      isActive={page === pageNum}
                      className="cursor-pointer"
                    >
                      {pageNum}
                    </PaginationLink>
                  </PaginationItem>
                ),
              )}
              {page < totalPages && (
                <PaginationItem>
                  <PaginationNext
                    onClick={() => setPage(page + 1)}
                    className="cursor-pointer"
                  />
                </PaginationItem>
              )}
            </PaginationContent>
          </Pagination>
        </div>
      )}
    </div>
  )
}
