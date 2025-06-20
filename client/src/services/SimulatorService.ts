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
  mode?: 'real' | 'simulation'
}) => {
  const { simulationId, endTimeOrders, startTimeOrders, mode } = params
  await simulatorService.startSimulation(simulationId, {
    endTimeOrders,
    startTimeOrders,
    mode: mode ?? 'simulation',
  })
}

export const stopSimulation = async (simulationId: string) => {
  await simulatorService.stopSimulation(simulationId)
}

export const commandSimulation = async (
  simulationId: string,
  command: 'PAUSE' | 'RESUME' | 'DESACCELERATE' | 'ACCELERATE',
) => {
  await simulatorService.sendCommand(simulationId, {
    command,
  })
}
