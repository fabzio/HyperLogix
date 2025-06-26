import type { PLGNetwork } from '@/domain/PLGNetwork'
import type { Routes } from '@/domain/Routes'
import { useOperationStore } from '@/features/dashboard/store/operation'
import { useWebSocketStore } from '@/store/websocket'
import { useCallback, useEffect } from 'react'

type OperationMetrics = {
  fleetUtilizationPercentage: number
  averageFuelConsumptionPerKm: number
  completionPercentage: number
  averageDeliveryTimeMinutes: number
  averageCapacityUtilization: number
  averagePlanificationTimeSeconds: number
  totalDistanceTraveled: number
  deliveryEfficiencyPercentage: number
  totalOrdersProcessed: number
  pendingOrdersCount: number
}

type MessageResponse = {
  timestamp: string
  simulationTime: string
  plgNetwork: PLGNetwork
  planificationStatus?: {
    planning: boolean
    currentNodesProcessed: number
  }
  routes?: Routes
  metrics?: OperationMetrics
}

const OPERATION_SESSION_ID = 'main' as const

export const useWatchOperation = () => {
  const {
    setState,
    plgNetwork,
    simulationTime,
    routes,
    metrics,
    planificationStatus,
  } = useOperationStore()
  const { subscribe, unsubscribe, connected, client } = useWebSocketStore()

  const handleMessage = useCallback(
    (message: unknown) => {
      try {
        const typedMessage = message as MessageResponse
        // Update operation store with real-time data from WebSocket
        setState({
          plgNetwork: typedMessage.plgNetwork,
          simulationTime: typedMessage.simulationTime,
          planificationStatus: typedMessage.planificationStatus || null,
          routes: typedMessage.routes || null,
          metrics: typedMessage.metrics || null,
          isConnected: true,
        })
        console.log('Operation message received:', typedMessage)
        console.log('Routes data:', typedMessage.routes)
        console.log('Orders data:', typedMessage.plgNetwork?.orders)
      } catch (error) {
        console.error('Error parsing operation message:', error)
        setState({ isConnected: false })
      }
    },
    [setState],
  )

  useEffect(() => {
    if (!client || !connected) {
      setState({ isConnected: false })
      return
    }

    subscribe(`/topic/simulation/${OPERATION_SESSION_ID}`, handleMessage)
    setState({
      isConnected: true,
      operationStartTime: new Date().toISOString(),
    })

    return () => {
      unsubscribe(`/topic/simulation/${OPERATION_SESSION_ID}`)
    }
  }, [subscribe, unsubscribe, connected, client, handleMessage, setState])

  return {
    plgNetwork,
    simulationTime,
    routes,
    metrics,
    planificationStatus,
    isConnected: useOperationStore().isConnected,
    lastOrderSubmitted: useOperationStore().lastOrderSubmitted,
    isSubmittingOrder: useOperationStore().isSubmittingOrder,
    operationStartTime: useOperationStore().operationStartTime,
    setState: useOperationStore().setState,
    addOrder: useOperationStore().addOrder,
    updateOrderStatus: useOperationStore().updateOrderStatus,
    setSubmittingOrder: useOperationStore().setSubmittingOrder,
    clearLastOrderSubmitted: useOperationStore().clearLastOrderSubmitted,
    reset: useOperationStore().reset,
  }
}
