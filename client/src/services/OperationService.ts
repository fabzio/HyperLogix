import { Configuration, OperationControllerApi } from '@/api'
import type { RegisterOrderRequest, TruckBreakdownRequest } from '@/api'
import http from '@/lib/http'

const repository = new OperationControllerApi(new Configuration(), '/', http)

export const registerOrder = async (orderRequest: RegisterOrderRequest) => {
  const { data } = await repository.registerOrder(orderRequest)
  return data
}

export const reportTruckBreakdown = async (
  truckId: string,
  request: TruckBreakdownRequest,
) => {
  const { data } = await repository.reportTruckBreakdown(truckId, request)
  return data
}

export const getOperationStatus = async () => {
  const { data } = await repository.getOperationStatus()
  return data
}

export const triggerManualReplanification = async () => {
  const { data } = await repository.manualReplanification()
  return data
}
