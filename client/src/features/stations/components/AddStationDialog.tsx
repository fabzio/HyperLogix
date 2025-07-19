import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
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
import { useCreateStation } from '@/features/stations/hooks/useStationMutations'
import { zodResolver } from '@hookform/resolvers/zod'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

const formSchema = z.object({
  name: z.string().min(1, 'El nombre es requerido'),
  maxCapacity: z.number().min(1, 'La capacidad debe ser mayor a 0').optional(),
  mainStation: z.boolean(),
  locationX: z
    .number()
    .min(0, 'X debe ser mayor o igual a 0')
    .max(70, 'X debe ser menor o igual a 70'),
  locationY: z
    .number()
    .min(0, 'Y debe ser mayor o igual a 0')
    .max(50, 'Y debe ser menor o igual a 50'),
})

type StationFormData = z.infer<typeof formSchema>

export default function AddStationDialog() {
  const [open, setOpen] = useState(false)
  const { mutate: createStation, isPending } = useCreateStation()

  const form = useForm<StationFormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      maxCapacity: 160,
      mainStation: false,
      locationX: 0,
      locationY: 0,
    },
  })

  const onSubmit = (data: StationFormData) => {
    createStation(
      {
        name: data.name,
        maxCapacity: data.mainStation
          ? Number.MAX_SAFE_INTEGER
          : data.maxCapacity || 160,
        mainStation: data.mainStation,
        location: { x: data.locationX, y: data.locationY },
        id: '',
        availableCapacityPerDate: {},
        reservationHistory: [],
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
    const names = [
      'Estación Norte',
      'Estación Sur',
      'Estación Este',
      'Estación Oeste',
      'Estación Central',
    ]
    const randomName = names[Math.floor(Math.random() * names.length)]
    const randomCapacity = Math.floor(Math.random() * 200) + 100
    const isMainStation = Math.random() > 0.8

    form.setValue('name', `${randomName} ${Math.floor(Math.random() * 100)}`)
    form.setValue('maxCapacity', isMainStation ? undefined : randomCapacity)
    form.setValue('mainStation', isMainStation)
    form.setValue('locationX', Math.floor(Math.random() * 70))
    form.setValue('locationY', Math.floor(Math.random() * 50))
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4 mr-2" />
          Agregar Estación
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Agregar Estación</DialogTitle>
          <DialogDescription>
            Complete los datos de la estación. Los campos marcados con * son
            obligatorios.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <div className="flex gap-2">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem className="flex-1">
                    <FormLabel>Nombre *</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="Estación Central" />
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

            <FormField
              control={form.control}
              name="maxCapacity"
              render={({ field }) => {
                const isMainStation = form.watch('mainStation')
                return (
                  <FormItem>
                    <FormLabel>
                      Capacidad Máxima (m³) *
                      {isMainStation && (
                        <span className="text-sm font-normal text-muted-foreground ml-2">
                          (Infinita para estación principal)
                        </span>
                      )}
                    </FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => {
                          const value = e.target.value
                          field.onChange(value ? Number(value) : undefined)
                        }}
                        placeholder={isMainStation ? '∞' : '160'}
                        disabled={isMainStation}
                        value={isMainStation ? '' : field.value || ''}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )
              }}
            />

            <FormField
              control={form.control}
              name="mainStation"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                  <div className="space-y-0.5">
                    <FormLabel className="text-base">
                      Estación Principal
                    </FormLabel>
                    <FormMessage />
                  </div>
                  <FormControl>
                    <Checkbox
                      checked={field.value}
                      onCheckedChange={field.onChange}
                    />
                  </FormControl>
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="locationX"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Coordenada X (0-70)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="35"
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
                name="locationY"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Coordenada Y (0-50)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(Number(e.target.value))}
                        placeholder="25"
                        min="0"
                        max="50"
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
                {isPending ? 'Creando...' : 'Crear Estación'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
