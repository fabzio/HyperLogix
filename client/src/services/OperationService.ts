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

export const reportTruckMaintenance = async (
  truckId: string,
  request: TruckBreakdownRequest,
) => {
  const { data } = await repository.reportTruckMaintenance(truckId, request)
  return data
}

export const restoreTruckToIdle = async (truckId: string) => {
  const { data } = await repository.restoreTruckToIdle(truckId)
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

export const sendSimulationCommand = async (
  command: 'PAUSE' | 'RESUME' | 'ACCELERATE' | 'DESACCELERATE',
) => {
  switch (command) {
    case 'PAUSE':
      await repository.pauseSimulation()
      break
    case 'RESUME':
      await repository.resumeSimulation()
      break
    case 'ACCELERATE':
      await repository.accelerateSimulation()
      break
    case 'DESACCELERATE':
      await repository.decelerateSimulation()
      break
    default:
      throw new Error(`Unknown command: ${command}`)
  }
}

export const cancelOrder = async (orderId: string) => {
  const { data } = await repository.cancelOrder(orderId)
  return data
}
