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
import { useSimulationStore } from '@/features/simulation/store/simulation'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useState } from 'react'
import OrderDetail from './OrderDetail'

export default function Orders() {
  const { truckId, orderId } = useSearch({ from: '/_auth/simulacion' })
  const { plgNetwork, routes, simulationTime } = useSimulationStore()
  const navigate = useNavigate({ from: '/simulacion' })
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const orders =
    plgNetwork?.orders.filter((order) => {
      if (!truckId) return true
      const stops = routes?.stops[truckId] || []
      return stops.some((stop) => stop.node.id === order.id)
    }) || []
  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedOrders = orders.slice(startIndex, endIndex)
  const totalPages = Math.ceil(orders.length / pageSize)
  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setPage(newPage)
    }
  }
  const paginationNumbers = generatePaginationNumbers(page, totalPages)
  if (!plgNetwork || !routes || !simulationTime) {
    return <div>Cargando...</div>
  }

  return (
    <article>
      <Typography variant="h3">Pedidos</Typography>
      {!orderId ? (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  ID
                </TableHead>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  Estado
                </TableHead>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  GLP
                </TableHead>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  Tiempo Restante
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.length > 0 ? (
                paginatedOrders.map((order) => {
                  const maxDate = new Date(order.maxDeliveryDate)
                  const simulationDate = new Date(simulationTime)
                  const remainingTime = Math.max(
                    Math.ceil(
                      (maxDate.getTime() - simulationDate.getTime()) /
                        (1000 * 60 * 60),
                    ),
                    0,
                  )
                  return (
                    <TableRow
                      key={order.id}
                      className="hover:cursor-pointer"
                      onClick={() =>
                        navigate({
                          to: '/simulacion',
                          search: { orderId: order.id },
                        })
                      }
                    >
                      <TableCell className="px-4 py-2 font-mono">
                        #{order.id.slice(-5)}
                      </TableCell>
                      <TableCell className="px-4 py-2">
                        {order.status === 'PENDING' && 'Pendiente'}
                        {order.status === 'CALCULATING' && 'Calculando'}
                        {order.status === 'IN_PROGRESS' && 'En Progreso'}
                        {order.status === 'COMPLETED' && 'Completado'}
                      </TableCell>
                      <TableCell className="px-4 py-2">
                        {order.deliveredGLP.toString().padStart(2, ' ')}/
                        {order.requestedGLP.toString().padStart(2, ' ')}
                      </TableCell>
                      <TableCell className="px-4 py-2">
                        {order.status === 'COMPLETED'
                          ? 'A tiempo'
                          : remainingTime > 0
                            ? `${remainingTime} horas`
                            : 'Retrasado'}
                      </TableCell>
                    </TableRow>
                  )
                })
              ) : (
                <TableRow>
                  <TableCell colSpan={4} className="text-center">
                    No hay órdenes disponibles
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
          <Pagination>
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious
                  onClick={() => handlePageChange(page - 1)}
                  aria-disabled={page === 1}
                  className={page === 1 ? 'opacity-50 pointer-events-none' : ''}
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
                    className={page === pageNum ? 'active font-bold' : ''}
                    style={{ cursor: 'pointer' }}
                  >
                    {pageNum}
                  </PaginationLink>
                ),
              )}
              <PaginationItem>
                <PaginationNext
                  onClick={() => handlePageChange(page + 1)}
                  aria-disabled={page === totalPages}
                  className={
                    page === totalPages ? 'opacity-50 pointer-events-none' : ''
                  }
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </>
      ) : (
        <OrderDetail />
      )}
    </article>
  )
}

const generatePaginationNumbers = (currentPage: number, totalPages: number) => {
  const surroundingPageCount = 1
  const finalPageNumbers: (number | string)[] = []
  let previousPage: number | undefined

  for (let pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
    const isPageAtStartOrEnd = pageNumber === 1 || pageNumber === totalPages
    const isPageNearCurrent =
      pageNumber >= currentPage - surroundingPageCount &&
      pageNumber <= currentPage + surroundingPageCount

    if (isPageAtStartOrEnd || isPageNearCurrent) {
      if (previousPage !== undefined) {
        const isThereAGap = pageNumber - previousPage === 2
        const isThereALargerGap = pageNumber - previousPage > 2

        if (isThereAGap) {
          finalPageNumbers.push(previousPage + 1)
        } else if (isThereALargerGap) {
          finalPageNumbers.push('...')
        }
      }

      finalPageNumbers.push(pageNumber)
      previousPage = pageNumber
    }
  }

  return finalPageNumbers
}
