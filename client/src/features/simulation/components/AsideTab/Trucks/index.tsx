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
import TruckDetail from './TruckDetail'

export default function Trucks() {
  const { truckId } = useSearch({ from: '/_auth/simulacion' })
  const navigate = useNavigate({ from: '/simulacion' })
  const { plgNetwork } = useSimulationStore()
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)

  const trucks =
    plgNetwork?.trucks.sort(
      (t1, t2) =>
        t1.currentCapacity / t1.maxCapacity -
        t2.currentCapacity / t2.maxCapacity,
    ) || []

  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedTrucks = trucks.slice(startIndex, endIndex)
  const totalPages = Math.ceil(trucks.length / pageSize)

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setPage(newPage)
    }
  }

  const paginationNumbers = generatePaginationNumbers(page, totalPages)

  return (
    <article>
      <Typography variant="h3">Camiones</Typography>
      {!truckId ? (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  Código
                </TableHead>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  Combustible
                </TableHead>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  Capacidad
                </TableHead>
                <TableHead className="px-4 py-2 text-left font-semibold">
                  Ubicación
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {trucks.length > 0 ? (
                paginatedTrucks.map((truck) => (
                  <TableRow
                    key={truck.code}
                    className="hover:cursor-pointer"
                    onClick={() => {
                      navigate({
                        to: '/simulacion',
                        search: { truckId: +truck.id },
                      })
                    }}
                  >
                    <TableCell className="px-4 py-2">{truck.code}</TableCell>
                    <TableCell className="px-4 py-2">
                      {Math.floor(truck.currentFuel)
                        .toString()
                        .padStart(2, ' ')}{' '}
                      /{' '}
                      {Math.floor(truck.fuelCapacity)
                        .toString()
                        .padStart(2, ' ')}
                    </TableCell>
                    <TableCell className="px-4 py-2">
                      {Math.floor(truck.currentCapacity)
                        .toString()
                        .padStart(2, ' ')}{' '}
                      /{' '}
                      {Math.floor(truck.maxCapacity)
                        .toString()
                        .padStart(2, ' ')}
                    </TableCell>
                    <TableCell className="px-4 py-2">
                      {Math.floor(truck.location.x).toString().padStart(2, ' ')}
                      ,{' '}
                      {Math.floor(truck.location.y).toString().padStart(2, ' ')}
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={4} className="text-center px-4 py-2">
                    No hay camiones disponibles
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
        <TruckDetail truckId={truckId.toString()} />
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
