import { BenchmarkControllerApi } from '@/api'
import type { Order } from '@/domain/Order'
import type { Station } from '@/domain/Station'
import type { Truck } from '@/domain/Truck'
import http from '@/lib/http'

const repository = new BenchmarkControllerApi(undefined, '/', http)

export const StartBenchmark = async () => {
  const response = await repository.startBenchmark()
  const trucks = response.data.trucks?.map((truck) => truck as Truck) || []
  const stations =
    response.data.stations?.map((station) => station as Station) || []
  const orders = response.data.orders?.map((order) => order as Order) || []
  return {
    trucks,
    stations,
    orders,
  }
}
