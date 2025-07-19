import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { zodResolver } from '@hookform/resolvers/zod'
import { ShieldAlert } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

const createBlockadeSchema = z.object({
  nodeId: z.string().min(1, 'El ID del nodo es requerido'),
  reason: z.string().min(1, 'La razón del bloqueo es requerida'),
  startTime: z.string().min(1, 'La hora de inicio es requerida'),
  endTime: z.string().min(1, 'La hora de fin es requerida'),
  description: z.string().optional(),
})

type CreateBlockadeFormData = z.infer<typeof createBlockadeSchema>

export function CreateBlockadeDialog() {
  const [isOpen, setIsOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const form = useForm<CreateBlockadeFormData>({
    resolver: zodResolver(createBlockadeSchema),
    defaultValues: {
      nodeId: '',
      reason: '',
      startTime: '',
      endTime: '',
      description: '',
    },
  })

  const onSubmit = async (data: CreateBlockadeFormData) => {
    setIsSubmitting(true)
    try {
      // TODO: Implementar la creación del bloqueo
      console.log('Creando bloqueo:', data)

      // Simular API call
      await new Promise((resolve) => setTimeout(resolve, 1000))

      toast.success('Bloqueo creado exitosamente')
      setIsOpen(false)
      form.reset()
    } catch (error) {
      toast.error('Error al crear el bloqueo')
      console.error('Error creating blockade:', error)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        <Button
          variant="outline"
          className="flex flex-col h-20 items-center justify-center gap-2"
        >
          <ShieldAlert className="h-5 w-5 text-orange-500" />
          <span className="text-xs">Crear Bloqueo</span>
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Crear Nuevo Bloqueo</DialogTitle>
          <DialogDescription>
            Crea un bloqueo temporal en un nodo específico de la red.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="nodeId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>ID del Nodo</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: NODE_001" {...field} />
                  </FormControl>
                  <FormDescription>
                    Identificador único del nodo donde se aplicará el bloqueo
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Razón del Bloqueo</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: Mantenimiento de vía" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="startTime"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Hora de Inicio</FormLabel>
                    <FormControl>
                      <Input type="datetime-local" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="endTime"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Hora de Fin</FormLabel>
                    <FormControl>
                      <Input type="datetime-local" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descripción (Opcional)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Detalles adicionales sobre el bloqueo..."
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsOpen(false)}
                disabled={isSubmitting}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Creando...' : 'Crear Bloqueo'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
