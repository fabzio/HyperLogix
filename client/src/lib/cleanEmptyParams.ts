const DEFAULT_PAGE_INDEX = 0
const DEFAULT_PAGE_SIZE = 10

export const cleanEmptyParams = <T extends Record<string, unknown>>(
  search: T,
) => {
  const newSearch = { ...search }
  for (const key of Object.keys(newSearch)) {
    const value = newSearch[key]
    if (
      value === undefined ||
      value === '' ||
      (typeof value === 'number' && Number.isNaN(value))
    ) {
      delete newSearch[key]
    }
  }

  // biome-ignore lint/suspicious/noExplicitAny: <explanation>
  if ((search as any)?.pageIndex === DEFAULT_PAGE_INDEX)
    // biome-ignore lint/suspicious/noExplicitAny: <explanation>
    (newSearch as any).pageIndex = undefined
  // biome-ignore lint/suspicious/noExplicitAny: <explanation>
  if ((search as any)?.pageSize === DEFAULT_PAGE_SIZE)
    // biome-ignore lint/suspicious/noExplicitAny: <explanation>
    (newSearch as any).pageSize = undefined

  return newSearch
}
