import {
  Accordion,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import { AccordionContent } from '@radix-ui/react-accordion'
import { useState } from 'react'
import { useSimulationStore } from '../../store/simulation'

type TruckRoute = {
  truck: {
    id: string
    code: string
  }
  stops: Array<{
    id: string
    type: 'station' | 'order'
    description: string
    location: { x: number; y: number }
  }>
}

export default function Routes() {
  const { routes, plgNetwork } = useSimulationStore()
  if (!plgNetwork) return null
  if (!routes) return null
  const { trucks, orders, stations } = plgNetwork
  const filterRoutes: TruckRoute[] = []

  for (const truck of trucks) {
    const { stops } = routes
    const truckStops = stops[truck.id] || []
    const truckStopsWithDetails = truckStops.map((stop) => {
      const order = orders.find((o) => o.id === stop.node.id)
      const station = stations.find((s) => s.id === stop.node.id)
      return {
        id: stop.node.id,
        type: order ? ('order' as const) : ('station' as const),
        description: order ? order.clientId : station?.name || 'Unknown',
        location: stop.node.location,
      }
    })
    filterRoutes.push({
      truck: {
        id: truck.id,
        code: truck.code,
      },
      stops: truckStopsWithDetails,
    })
  }

  const [page, setPage] = useState(1)
  const [pageSize] = useState(5)
  const totalRoutes = filterRoutes.length
  const totalPages = Math.ceil(totalRoutes / pageSize)
  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedRoutes = filterRoutes.slice(startIndex, endIndex)

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setPage(newPage)
    }
  }

  const paginationNumbers = generatePaginationNumbers(page, totalPages)

  return (
    <div>
      <Accordion type="single">
        {paginatedRoutes.map((route) => (
          <AccordionItem
            value={route.truck.code}
            key={route.truck.id}
            className="mb-4"
          >
            <AccordionTrigger className="text-lg font-semibold mb-2">
              Truck {route.truck.code}
            </AccordionTrigger>
            <AccordionContent>
              <ul className="list-disc pl-5">
                {route.stops.map((stop) => (
                  <li key={stop.id}>
                    {stop.type === 'order' ? (
                      <span>
                        Peiddo: {stop.description} - ({stop.location.x},{' '}
                        {stop.location.y})
                      </span>
                    ) : (
                      <span>
                        Estacion: {stop.description} - ({stop.location.x},{' '}
                        {stop.location.y})
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </AccordionContent>
          </AccordionItem>
        ))}
      </Accordion>
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
    </div>
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
