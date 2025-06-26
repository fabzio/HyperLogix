import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
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
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { useRegisterOrder } from '@/features/dashboard/hooks/useOperationMutations'
import { cn } from '@/lib/utils'
import { zodResolver } from '@hookform/resolvers/zod'
import { format } from 'date-fns'
import { CalendarIcon, Package, Shuffle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

const formSchema = z.object({
  id: z.string().min(1, 'ID es obligatorio'),
  clientId: z.string().min(1, 'ID del cliente es obligatorio'),
  requestedGLP: z.number().min(1, 'Cantidad de GLP debe ser mayor a 0'),
  location: z.object({
    x: z.number().min(0, 'X debe ser >= 0').max(70, 'X debe ser <= 70'),
    y: z.number().min(0, 'Y debe ser >= 0').max(50, 'Y debe ser <= 50'),
  }),
  date: z.date({
    required_error: 'Fecha de pedido es obligatoria',
  }),
  deliveryLimit: z.date({
    required_error: 'Fecha límite de entrega es obligatoria',
  }),
})

type FormSchema = z.infer<typeof formSchema>

export default function AddOrderDialog() {
  const [open, setOpen] = useState(false)
  const registerOrderMutation = useRegisterOrder()

  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      id: '',
      clientId: '',
      requestedGLP: 100,
      location: {
        x: 12,
        y: 8,
      },
      date: new Date(),
      deliveryLimit: new Date(Date.now() + 24 * 60 * 60 * 1000), // Tomorrow
    },
  })

  const generateRandomOrder = () => {
    const randomId = `ORD-${Date.now()}`
    const randomClientId = `C-${Math.floor(Math.random() * 100)}`

    // Generate random coordinates near (12, 8) with radius between 1 and 5
    const centerX = 12
    const centerY = 8
    const minRadius = 1
    const maxRadius = 5

    // Generate random angle and radius
    const angle = Math.random() * 2 * Math.PI
    const radius = minRadius + Math.random() * (maxRadius - minRadius)

    // Calculate coordinates and ensure they're within bounds
    let randomX = Math.round(centerX + radius * Math.cos(angle))
    let randomY = Math.round(centerY + radius * Math.sin(angle))

    // Ensure coordinates stay within valid bounds (0-70 for X, 0-50 for Y)
    randomX = Math.max(0, Math.min(70, randomX))
    randomY = Math.max(0, Math.min(50, randomY))

    const randomGLP = Math.floor(Math.random() * 30) + 1 // 1-30 L
    const now = new Date()
    const randomDeliveryLimit = new Date(
      now.getTime() + (Math.random() * 7 + 1) * 24 * 60 * 60 * 1000,
    ) // 1-8 days

    form.setValue('id', randomId)
    form.setValue('clientId', randomClientId)
    form.setValue('requestedGLP', randomGLP)
    form.setValue('location.x', randomX)
    form.setValue('location.y', randomY)
    form.setValue('date', now)
    form.setValue('deliveryLimit', randomDeliveryLimit)
  }

  const onSubmit = async (values: FormSchema) => {
    try {
      await registerOrderMutation.mutateAsync({
        id: values.id,
        clientId: values.clientId,
        requestedGLP: values.requestedGLP,
        location: values.location,
        date: values.date.toISOString(),
        deliveryLimit: `PT${Math.floor((values.deliveryLimit.getTime() - values.date.getTime()) / (1000 * 60 * 60))}H`, // Duration in hours
      })

      toast.success('Orden registrada exitosamente')
      setOpen(false)
      form.reset()
    } catch (error) {
      toast.error('Error al registrar la orden')
      console.error('Error registering order:', error)
    }
  }

  // Auto-generate ID when dialog opens
  useEffect(() => {
    if (open && !form.getValues('id')) {
      form.setValue('id', `ORD-${Date.now()}`)
    }
  }, [open, form])

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          variant="outline"
          className="flex flex-col h-20 items-center justify-center gap-2"
        >
          <Package className="h-5 w-5 text-green-500" />
          <span className="text-xs">Nueva Orden</span>
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Registrar Nueva Orden</DialogTitle>
          <DialogDescription>
            Complete los datos de la orden para la operación en tiempo real.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <div className="flex gap-2">
              <FormField
                control={form.control}
                name="id"
                render={({ field }) => (
                  <FormItem className="flex-1">
                    <FormLabel>ID de Orden</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="ORD-12345" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-8"
                onClick={generateRandomOrder}
                title="Generar datos aleatorios"
              >
                <Shuffle className="h-4 w-4" />
              </Button>
            </div>

            <FormField
              control={form.control}
              name="clientId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>ID del Cliente</FormLabel>
                  <FormControl>
                    <Input {...field} placeholder="CLIENT-123" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="requestedGLP"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cantidad GLP (L)</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      {...field}
                      onChange={(e) => field.onChange(Number(e.target.value))}
                      placeholder="100"
                      min="1"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="location.x"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Coordenada X (0-70)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="12"
                        min="0"
                        max="70"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="location.y"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Coordenada Y (0-50)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="8"
                        min="0"
                        max="50"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="date"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha de Pedido</FormLabel>
                  <Popover>
                    <PopoverTrigger asChild>
                      <FormControl>
                        <Button
                          variant="outline"
                          className={cn(
                            'w-full pl-3 text-left font-normal',
                            !field.value && 'text-muted-foreground',
                          )}
                        >
                          {field.value ? (
                            format(field.value, 'PPP')
                          ) : (
                            <span>Seleccione una fecha</span>
                          )}
                          <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                        </Button>
                      </FormControl>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        mode="single"
                        selected={field.value}
                        onSelect={field.onChange}
                        disabled={(date) => date < new Date('2024-01-01')}
                        initialFocus
                      />
                    </PopoverContent>
                  </Popover>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="deliveryLimit"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha Límite de Entrega</FormLabel>
                  <Popover>
                    <PopoverTrigger asChild>
                      <FormControl>
                        <Button
                          variant="outline"
                          className={cn(
                            'w-full pl-3 text-left font-normal',
                            !field.value && 'text-muted-foreground',
                          )}
                        >
                          {field.value ? (
                            format(field.value, 'PPP')
                          ) : (
                            <span>Seleccione una fecha</span>
                          )}
                          <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                        </Button>
                      </FormControl>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        mode="single"
                        selected={field.value}
                        onSelect={field.onChange}
                        disabled={(date) => date < new Date()}
                        initialFocus
                      />
                    </PopoverContent>
                  </Popover>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setOpen(false)}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={registerOrderMutation.isPending}>
                {registerOrderMutation.isPending
                  ? 'Registrando...'
                  : 'Registrar Orden'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
