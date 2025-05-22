import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  type ColumnDef,
  type OnChangeFn,
  type PaginationOptions,
  type PaginationState,
  type SortingState,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table'
import { Button } from '../ui/button'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
} from '../ui/pagination'

type Props<T extends Record<string, unknown>> = {
  data: T[]
  columns: ColumnDef<T>[]
  pagination: PaginationState
  paginationOptions: Pick<
    PaginationOptions,
    'onPaginationChange' | 'rowCount' | 'pageCount'
  >
  sorting: SortingState
  onSortingChange: OnChangeFn<SortingState>
  rowSelection: Record<string, boolean>
  setRowSelection: OnChangeFn<Record<string, boolean>>
}

export default function DataTable<T extends Record<string, unknown>>({
  data,
  columns,
  pagination,
  paginationOptions,
  sorting,
  onSortingChange,
  rowSelection,
  setRowSelection,
}: Props<T>) {
  const table = useReactTable({
    data,
    columns,
    manualFiltering: true,
    manualSorting: true,
    manualPagination: true,
    onSortingChange,
    ...paginationOptions,
    filterFns: {
      fuzzy: (row, columnId, value) => {
        const rowValue = row.getValue(columnId)
        if (typeof rowValue === 'string') {
          return rowValue.toLowerCase().includes(value.toLowerCase())
        }
        return false
      },
    },
    getCoreRowModel: getCoreRowModel(),
    onRowSelectionChange: setRowSelection,
    enableRowSelection: rowSelection !== undefined,
    state: {
      pagination,
      sorting,
      rowSelection: rowSelection,
    },
  })
  const currentPage = table.getState().pagination.pageIndex
  const totalPages = table.getPageCount()
  const paginationNumbers = generatePaginationNumbers(
    currentPage + 1,
    totalPages,
  )
  return (
    <>
      <Table>
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <TableHead key={header.id}>
                  {header.isPlaceholder
                    ? null
                    : flexRender(
                        header.column.columnDef.header,
                        header.getContext(),
                      )}
                </TableHead>
              ))}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows.length ? (
            table.getRowModel().rows.map((row) => (
              <TableRow
                key={row.id}
                data-state={row.getIsSelected() && 'selected'}
              >
                {row.getVisibleCells().map((cell) => (
                  <TableCell key={cell.id}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </TableCell>
                ))}
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell colSpan={columns.length} className="text-center">
                No se encontraron resultados
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>

      <Pagination className="flex justify-center my-4 space-x-2">
        <PaginationContent>
          <PaginationItem>
            <Button
              variant="secondary"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
            >
              Anterior
            </Button>
          </PaginationItem>

          {paginationNumbers.map((page) => (
            <PaginationItem
              key={
                typeof page === 'number' ? `page-${page}` : `ellipsis-${page}`
              }
            >
              {page === '...' ? (
                <PaginationEllipsis />
              ) : (
                <PaginationItem>
                  <Button
                    variant={page === currentPage + 1 ? 'outline' : 'ghost'}
                    size="icon"
                    onClick={() => table.setPageIndex(+page - 1)}
                  >
                    {page}
                  </Button>
                </PaginationItem>
              )}
            </PaginationItem>
          ))}
          <PaginationItem>
            <Button
              variant="secondary"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
            >
              Siguiente
            </Button>
          </PaginationItem>
        </PaginationContent>
      </Pagination>
    </>
  )
}
const generatePaginationNumbers = (currentPage: number, totalPages: number) => {
  const surroundingPageCount = 2
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
