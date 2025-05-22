export type Paginated<T> = {
  content: T[]
  first: boolean
  last: boolean
  number: number
  totalPages: number
  totalElements: number
}
