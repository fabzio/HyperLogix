import { Configuration, SimulationControllerApi } from '@/api'
import http from '@/lib/http'

const simulatorService = new SimulationControllerApi(
  new Configuration(),
  '/',
  http,
)

export const getSimulationStatus = async (simulationId: string) => {
  const { data } = await simulatorService.getSimulationStatus(simulationId)
  return data
}

export const startSimulation = async (params: {
  simulationId: string
  endTimeOrders: string
  startTimeOrders: string
}) => {
  const { simulationId, endTimeOrders, startTimeOrders } = params
  await simulatorService.startSimulation(simulationId, {
    endTimeOrders,
    startTimeOrders,
  })
}

export const stopSimulation = async (simulationId: string) => {
  await simulatorService.stopSimulation(simulationId)
}
