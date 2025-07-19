import { SearchFilter } from '@/components/SearchFilter'
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
import { TruckState } from '@/domain/TruckState'
import { useNavigate, useSearch } from '@tanstack/react-router'
import { useState } from 'react'
import { useWatchOperation } from '../../../hooks/useOperation'
import TruckDetail from './TruckDetail'

export default function Trucks() {
  const { truckId } = useSearch({ from: '/_auth/map' })
  const navigate = useNavigate({ from: '/map' })
  const { plgNetwork } = useWatchOperation()
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [searchFilter, setSearchFilter] = useState('')

  const allTrucks =
    plgNetwork?.trucks.filter((truck) => truck.status !== TruckState.IDLE) || []

  const filteredTrucks = allTrucks.filter((truck) =>
    truck.code.toLowerCase().includes(searchFilter.toLowerCase()),
  )

  const trucks = filteredTrucks.sort(
    (t1, t2) =>
      t1.currentCapacity / t1.maxCapacity - t2.currentCapacity / t2.maxCapacity,
  )

  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedTrucks = trucks.slice(startIndex, endIndex)
  const totalPages = Math.ceil(trucks.length / pageSize)

  const getStatusColor = (status: TruckState) => {
    switch (status) {
      case TruckState.ACTIVE:
        return 'bg-green-500'
      case TruckState.MAINTENANCE:
        return 'bg-yellow-500'
      case TruckState.BROKEN_DOWN:
        return 'bg-red-500'
      case TruckState.IDLE:
        return 'bg-gray-500'
      default:
        return 'bg-gray-500'
    }
  }

  const handleTruckClick = (clickedTruckId: string) => {
    if (truckId?.toString() === clickedTruckId) {
      navigate({ search: {} })
    } else {
      navigate({ search: { truckId: Number(clickedTruckId) } })
    }
  }

  if (truckId) {
    const selectedTruck = allTrucks.find((t) => t.id === truckId.toString())
    if (selectedTruck) {
      return <TruckDetail />
    }
  }

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b">
        <Typography variant="h3">Camiones Activos</Typography>
        <p className="text-sm text-muted-foreground">
          {trucks.length} camiones en operación
        </p>
      </div>

      <div className="p-4">
        <SearchFilter
          value={searchFilter}
          onChange={setSearchFilter}
          placeholder="Buscar camión..."
        />
      </div>

      <div className="flex-1 overflow-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Código</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead>Combustible</TableHead>
              <TableHead>Carga</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {paginatedTrucks.map((truck) => (
              <TableRow
                key={truck.id}
                className={`cursor-pointer hover:bg-muted ${
                  truckId?.toString() === truck.id ? 'bg-muted' : ''
                }`}
                onClick={() => handleTruckClick(truck.id)}
              >
                <TableCell className="font-medium">{truck.code}</TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <div
                      className={`w-2 h-2 rounded-full ${getStatusColor(truck.status)}`}
                    />
                    <span className="text-sm">{truck.status}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="text-sm">
                      {truck.currentFuel.toFixed(1)}/{truck.fuelCapacity}
                    </span>
                    <div className="w-full bg-gray-200 rounded-full h-1.5">
                      <div
                        className="bg-blue-600 h-1.5 rounded-full"
                        style={{
                          width: `${(truck.currentFuel / truck.fuelCapacity) * 100}%`,
                        }}
                      />
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="text-sm">
                      {truck.currentCapacity}/{truck.maxCapacity}L
                    </span>
                    <div className="w-full bg-gray-200 rounded-full h-1.5">
                      <div
                        className="bg-green-600 h-1.5 rounded-full"
                        style={{
                          width: `${(truck.currentCapacity / truck.maxCapacity) * 100}%`,
                        }}
                      />
                    </div>
                  </div>
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
