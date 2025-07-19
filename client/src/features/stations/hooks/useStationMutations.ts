import { Configuration, StationControllerApi } from '@/api'
import type { Station } from '@/domain/Station'
import http from '@/lib/http'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

const repository = new StationControllerApi(new Configuration(), '/', http)

export const useCreateStation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (station: Station) => repository.createStation(station),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      toast.success('Estación creada exitosamente')
    },
    onError: (error) => {
      toast.error('Error al crear la estación')
      console.error('Error creating station:', error)
    },
  })
}

export const useUpdateStation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, station }: { id: string; station: Station }) =>
      repository.updateStation(id, station),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      toast.success('Estación actualizada exitosamente')
    },
    onError: (error) => {
      toast.error('Error al actualizar la estación')
      console.error('Error updating station:', error)
    },
  })
}

export const useDeleteStation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => repository.deleteStation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      toast.success('Estación eliminada exitosamente')
    },
    onError: (error) => {
      toast.error('Error al eliminar la estación')
      console.error('Error deleting station:', error)
    },
  })
}
