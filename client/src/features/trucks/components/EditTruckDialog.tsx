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
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { Truck } from '@/domain/Truck'
import { useUpdateTruck } from '@/features/trucks/hooks/useTruckMutations'
import { zodResolver } from '@hookform/resolvers/zod'
import { Edit } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

const formSchema = z
  .object({
    code: z.string().min(1, 'El código es requerido'),
    type: z.enum(['TA', 'TB', 'TC', 'TD'], {
      required_error: 'El tipo es requerido',
    }),
    tareWeight: z.number().min(0, 'El peso debe ser mayor a 0'),
    maxCapacity: z.number().min(1, 'La capacidad debe ser mayor a 0'),
    currentCapacity: z
      .number()
      .min(0, 'La capacidad actual debe ser mayor o igual a 0'),
    fuelCapacity: z
      .number()
      .min(1, 'La capacidad de combustible debe ser mayor a 0'),
    status: z.enum(['ACTIVE', 'MAINTENANCE', 'BROKEN_DOWN', 'IDLE'], {
      required_error: 'El estado es requerido',
    }),
  })
  .refine((data) => data.currentCapacity <= data.maxCapacity, {
    message: 'La capacidad actual no puede ser mayor a la capacidad máxima',
    path: ['currentCapacity'],
  })

type TruckFormData = z.infer<typeof formSchema>

interface EditTruckDialogProps {
  truck: Truck
}

export default function EditTruckDialog({ truck }: EditTruckDialogProps) {
  const [open, setOpen] = useState(false)
  const { mutate: updateTruck, isPending } = useUpdateTruck()

  const form = useForm<TruckFormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      code: truck.code,
      type: truck.type as never,
      tareWeight: truck.tareWeight,
      maxCapacity: truck.maxCapacity,
      currentCapacity: truck.currentCapacity,
      fuelCapacity: truck.fuelCapacity,
      status: truck.status as never,
    },
  })

  const onSubmit = (data: TruckFormData) => {
    updateTruck(
      {
        id: truck.id,
        truck: {
          ...truck,
          code: data.code,
          type: data.type as never,
          status: data.status as never,
          tareWeight: data.tareWeight,
          maxCapacity: data.maxCapacity,
          currentCapacity: data.currentCapacity,
          fuelCapacity: data.fuelCapacity,
          // Mantener valores existentes para campos no editables
          currentFuel: truck.currentFuel,
          location: truck.location,
        },
      },
      {
        onSuccess: () => {
          setOpen(false)
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <Edit className="h-4 w-4 mr-2" />
          Editar
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Editar Camión</DialogTitle>
          <DialogDescription>
            Modifique los datos del camión {truck.code}.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Código *</FormLabel>
                  <FormControl>
                    <Input {...field} placeholder="TA-0001" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tipo *</FormLabel>
                    <FormControl>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Selecciona un tipo" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="TA">TA</SelectItem>
                          <SelectItem value="TB">TB</SelectItem>
                          <SelectItem value="TC">TC</SelectItem>
                          <SelectItem value="TD">TD</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="status"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estado *</FormLabel>
                    <FormControl>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Selecciona un estado" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="ACTIVE">Activo</SelectItem>
                          <SelectItem value="IDLE">Inactivo</SelectItem>
                          <SelectItem value="MAINTENANCE">
                            Mantenimiento
                          </SelectItem>
                          <SelectItem value="BROKEN_DOWN">Averiado</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="tareWeight"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Peso Tara (t) *</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        step="0.1"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="5.5"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="maxCapacity"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Capacidad Máx (m³) *</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="20"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="currentCapacity"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Capacidad Actual (m³)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="0"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="fuelCapacity"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Capacidad Combustible (gal) *</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="100"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setOpen(false)}
                disabled={isPending}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending ? 'Actualizando...' : 'Actualizar Camión'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
