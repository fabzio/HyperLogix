import { Configuration, TruckControllerApi } from '@/api'
import type { Truck } from '@/domain/Truck'
import http from '@/lib/http'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

const repository = new TruckControllerApi(new Configuration(), '/', http)

export const useCreateTruck = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (truck: Truck) => repository.createTruck(truck),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trucks'] })
      toast.success('Camión creado exitosamente')
    },
    onError: (error) => {
      toast.error('Error al crear el camión')
      console.error('Error creating truck:', error)
    },
  })
}

export const useUpdateTruck = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, truck }: { id: string; truck: Truck }) =>
      repository.updateTruck(id, truck),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trucks'] })
      toast.success('Camión actualizado exitosamente')
    },
    onError: (error) => {
      toast.error('Error al actualizar el camión')
      console.error('Error updating truck:', error)
    },
  })
}

export const useDeleteTruck = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => repository.deleteTruck(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trucks'] })
      toast.success('Camión eliminado exitosamente')
    },
    onError: (error) => {
      toast.error('Error al eliminar el camión')
      console.error('Error deleting truck:', error)
    },
  })
}
