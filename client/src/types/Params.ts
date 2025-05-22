export type PaginatedData<T> = {
  result: T[]
  rowCount: number
}

export type SearchParams = { q: string }
export type PaginationParams = { pageIndex: number; pageSize: number }
export type SortParams = { sortBy: `${string}.${'asc' | 'desc'}` }
export type Filters = Partial<
  SearchParams & PaginationParams & SortParams & { eqnumber?: number }
>
