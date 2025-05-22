import { useFilters } from '@/hooks/useFilters'
import { sortByToState, stateToSortBy } from '@/lib/table'
import type { Paginated } from '@/types/Paginated'
import type { RegisteredRouter, RouteIds } from '@tanstack/react-router'
import type { ColumnDef } from '@tanstack/react-table'
import { useState } from 'react'
import DataTable from './DataTable'
import SearchInput from './SearchInput'

interface Props<T extends Record<string, unknown>> {
  path: RouteIds<RegisteredRouter['routeTree']>
  Management: (props: {
    selectedRows: number[]
    resetSelectedRows: () => void
  }) => React.ReactElement
  Dialog: () => React.ReactElement
  columns: ColumnDef<T>[]
  data?: Paginated<T>
}
const DEFAULT_PAGE_INDEX = 0
const DEFAULT_PAGE_SIZE = 10

export default function EntityManagement<T extends Record<string, unknown>>({
  path,
  data,
  columns,
  Management,
  Dialog,
}: Props<T>) {
  const { filters, setFilters } = useFilters(path)
  const paginationState = {
    pageIndex:
      'pageIndex' in filters
        ? (filters.pageIndex ?? DEFAULT_PAGE_INDEX)
        : DEFAULT_PAGE_INDEX,
    pageSize:
      'pageSize' in filters
        ? (filters.pageSize ?? DEFAULT_PAGE_SIZE)
        : DEFAULT_PAGE_SIZE,
  }
  const sortingState = 'sortBy' in filters ? sortByToState(filters.sortBy) : []
  const [rowSelection, setRowSelection] = useState<Record<string, boolean>>({})
  const selectedRows = Object.keys(rowSelection)
    .filter((key) => rowSelection[key])
    .map((key) => Number.parseInt(key))
  const resetSelectedRows = () => setRowSelection({})
  return (
    <div className="flex flex-col my-6 p-4 gap-2 rounded-lg shadow-md">
      <div className="w-full flex flex-col md:flex-row justify-between gap-4">
        <SearchInput />
        <div className="flex gap-4">
          <Management
            selectedRows={selectedRows}
            resetSelectedRows={resetSelectedRows}
          />
          <Dialog />
        </div>
      </div>
      <DataTable
        columns={columns}
        pagination={paginationState}
        sorting={sortingState}
        data={data?.content ?? []}
        onSortingChange={(updateOrValue) => {
          const newSortingState =
            typeof updateOrValue === 'function'
              ? updateOrValue(sortingState)
              : updateOrValue
          return setFilters({
            ...filters,
            sortBy: stateToSortBy(newSortingState),
          })
        }}
        paginationOptions={{
          onPaginationChange: (pagination) => {
            const newPagination =
              typeof pagination === 'function'
                ? pagination(paginationState)
                : pagination
            setFilters({ ...filters, ...newPagination })
          },
          rowCount: data?.number || 0,
          pageCount: data?.totalPages || 0,
        }}
        rowSelection={rowSelection}
        setRowSelection={setRowSelection}
      />
    </div>
  )
}
