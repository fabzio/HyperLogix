import type { RegisterOrderRequest, TruckBreakdownRequest } from '@/api'
import QueryKeys from '@/const/QueryKeys'
import { useOperationStore } from '@/features/dashboard/store/operation'
import {
  getOperationStatus,
  registerOrder,
  reportTruckBreakdown,
  triggerManualReplanification,
} from '@/services/OperationService'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

export function useRegisterOrder() {
  const queryClient = useQueryClient()
  const { addOrder, setSubmittingOrder } = useOperationStore()

  return useMutation({
    mutationFn: registerOrder,
    onMutate: () => {
      setSubmittingOrder(true)
    },
    onSuccess: (_, variables: RegisterOrderRequest) => {
      // Add to local store for immediate UI feedback
      if (
        variables.id &&
        variables.clientId &&
        variables.location &&
        variables.date &&
        variables.requestedGLP &&
        variables.location.x !== undefined &&
        variables.location.y !== undefined
      ) {
        addOrder({
          id: variables.id,
          clientId: variables.clientId,
          location: { x: variables.location.x, y: variables.location.y },
          requestedGLP: variables.requestedGLP,
          deliveredGLP: 0,
          date: variables.date,
          limitTime: variables.deliveryLimit || '',
          maxDeliveryDate: variables.deliveryLimit || '',
          status: 'PENDING',
        })
      }
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: [QueryKeys.ORDERS] })
      setSubmittingOrder(false)
    },
    onError: () => {
      setSubmittingOrder(false)
    },
  })
}

export function useReportTruckBreakdown() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      truckId,
      request,
    }: { truckId: string; request: TruckBreakdownRequest }) =>
      reportTruckBreakdown(truckId, request),
    onSuccess: () => {
      // Invalidate trucks and operation queries
      queryClient.invalidateQueries({ queryKey: [QueryKeys.TRUCKS] })
      queryClient.invalidateQueries({ queryKey: [QueryKeys.OPERATION] })
    },
  })
}

export function useManualReplanification() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: triggerManualReplanification,
    onSuccess: () => {
      // Invalidate operation-related queries to get fresh data
      queryClient.invalidateQueries({ queryKey: [QueryKeys.OPERATION] })
    },
  })
}

export function useOperationStatus() {
  return useQuery({
    queryKey: [QueryKeys.OPERATION, 'status'],
    queryFn: getOperationStatus,
    refetchInterval: 30000, // Refetch every 30 seconds
  })
}
