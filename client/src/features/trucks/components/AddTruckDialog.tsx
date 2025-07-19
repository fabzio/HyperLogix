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
import { useCreateTruck } from '@/features/trucks/hooks/useTruckMutations'
import { zodResolver } from '@hookform/resolvers/zod'
import { Plus } from 'lucide-react'
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
  })
  .refine((data) => data.currentCapacity <= data.maxCapacity, {
    message: 'La capacidad actual no puede ser mayor a la capacidad máxima',
    path: ['currentCapacity'],
  })

type TruckFormData = z.infer<typeof formSchema>

export default function AddTruckDialog() {
  const [open, setOpen] = useState(false)
  const { mutate: createTruck, isPending } = useCreateTruck()

  const form = useForm<TruckFormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      code: '',
      type: 'TA',
      tareWeight: 0,
      maxCapacity: 0,
      currentCapacity: 0,
      fuelCapacity: 0,
    },
  })

  const onSubmit = (data: TruckFormData) => {
    createTruck(
      {
        code: data.code,
        type: data.type as never,
        tareWeight: data.tareWeight,
        maxCapacity: data.maxCapacity,
        currentCapacity: data.currentCapacity,
        fuelCapacity: data.fuelCapacity,
        // Campos por defecto - se asignan automáticamente
        status: 'ACTIVE' as never,
        id: '',
        nextMaintenance: new Date(
          Date.now() + 365 * 24 * 60 * 60 * 1000,
        ).toISOString(),
        currentFuel: data.fuelCapacity, // Tanque lleno por defecto
        location: { x: 0, y: 0 }, // Ubicación por defecto
      },
      {
        onSuccess: () => {
          setOpen(false)
          form.reset()
        },
      },
    )
  }

  const generateRandomData = () => {
    const types = ['TA', 'TB', 'TC', 'TD']
    const randomType = types[Math.floor(Math.random() * types.length)]
    const randomTareWeight = Math.floor(Math.random() * 5) + 2
    const randomMaxCapacity = Math.floor(Math.random() * 50) + 10
    const randomCurrentCapacity = Math.floor(Math.random() * randomMaxCapacity)
    const randomFuelCapacity = Math.floor(Math.random() * 200) + 50

    form.setValue(
      'code',
      `${randomType}${String(Math.floor(Math.random() * 900) + 100).padStart(3, '0')}`,
    )
    form.setValue('type', randomType as 'TA' | 'TB' | 'TC' | 'TD')
    form.setValue('tareWeight', randomTareWeight)
    form.setValue('maxCapacity', randomMaxCapacity)
    form.setValue('currentCapacity', randomCurrentCapacity)
    form.setValue('fuelCapacity', randomFuelCapacity)
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4 mr-2" />
          Agregar Camión
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Agregar Camión</DialogTitle>
          <DialogDescription>
            Complete los datos del camión. Los campos marcados con * son
            obligatorios.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <div className="flex gap-2">
              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem className="flex-1">
                    <FormLabel>Código *</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="TA-0001" />
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
                onClick={generateRandomData}
                title="Generar datos aleatorios"
              >
                <Plus className="h-4 w-4" />
              </Button>
            </div>

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
                {isPending ? 'Creando...' : 'Crear Camión'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
