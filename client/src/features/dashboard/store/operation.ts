import type { Order } from '@/domain/Order'
import type { PLGNetwork } from '@/domain/PLGNetwork'
import type { Routes } from '@/domain/Routes'
import { create } from 'zustand'

type PlanificationStatus = {
  planning: boolean
  currentNodesProcessed: number
}

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

export interface OperationStore {
  plgNetwork: PLGNetwork | null
  planificationStatus: PlanificationStatus | null
  simulationTime: string | null
  routes: Routes | null
  metrics: OperationMetrics | null
  operationStartTime: string | null
  isConnected: boolean
  lastOrderSubmitted: Order | null
  isSubmittingOrder: boolean

  setState: (state: Partial<OperationStore>) => void
  addOrder: (order: Order) => void
  updateOrderStatus: (orderId: string, status: Order['status']) => void
  setSubmittingOrder: (isSubmitting: boolean) => void
  clearLastOrderSubmitted: () => void
  reset: () => void
}

export const useOperationStore = create<OperationStore>((set) => ({
  plgNetwork: null,
  planificationStatus: null,
  simulationTime: null,
  routes: null,
  metrics: null,
  operationStartTime: null,
  isConnected: false,
  lastOrderSubmitted: null,
  isSubmittingOrder: false,

  setState: (state) => set((prev) => ({ ...prev, ...state })),

  addOrder: (order) =>
    set((prev) => ({
      ...prev,
      lastOrderSubmitted: order,
    })),

  updateOrderStatus: (orderId, status) =>
    set((prev) => {
      if (prev.plgNetwork?.orders) {
        const updatedOrders = prev.plgNetwork.orders.map((order) =>
          order.id === orderId ? { ...order, status } : order,
        )
        return {
          ...prev,
          plgNetwork: {
            ...prev.plgNetwork,
            orders: updatedOrders,
          },
        }
      }
      return prev
    }),

  setSubmittingOrder: (isSubmitting) =>
    set((prev) => ({
      ...prev,
      isSubmittingOrder: isSubmitting,
    })),

  clearLastOrderSubmitted: () =>
    set((prev) => ({
      ...prev,
      lastOrderSubmitted: null,
    })),

  reset: () =>
    set({
      plgNetwork: null,
      planificationStatus: null,
      simulationTime: null,
      routes: null,
      metrics: null,
      operationStartTime: null,
      isConnected: false,
      lastOrderSubmitted: null,
      isSubmittingOrder: false,
    }),
}))
