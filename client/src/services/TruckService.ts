import { Configuration, TruckControllerApi } from '@/api'
import type { Truck } from '@/domain/Truck'
import http from '@/lib/http'
import type { Paginated } from '@/types/Paginated'

const repository = new TruckControllerApi(new Configuration(), '/', http)

export const listTrucks = async (page = 0, size = 10) => {
  const { data } = await repository.list({ page, size })
  const parsedData: Paginated<Truck> = {
    ...(data as NonNullable<Paginated<Truck>>),
    content: (data?.content || []) as Truck[],
  }
  return parsedData
}
