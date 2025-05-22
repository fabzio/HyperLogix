import { Configuration, StationControllerApi } from '@/api'
import type { Station } from '@/domain/Station'
import http from '@/lib/http'
import type { Paginated } from '@/types/Paginated'

const repository = new StationControllerApi(new Configuration(), '/', http)

export const listStations = async (page = 0, size = 20) => {
  const { data } = await repository.list1({ page, size })
  const parsedData: Paginated<Station> = {
    ...(data as NonNullable<Paginated<Station>>),
    content: (data.content ?? []) as Station[],
  }
  return parsedData
}
