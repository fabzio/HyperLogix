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
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { useForm } from 'react-hook-form'

interface StationForm {
  name: string
  x: number
  y: number
  maxCapacity: number
  mainStation: boolean
}

export default function AddStationDialog() {
  const form = useForm<StationForm>({
    defaultValues: {
      name: '',
      x: 0,
      y: 0,
      maxCapacity: 160,
      mainStation: false,
    },
  })

  function onSubmit(values: StationForm) {}

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button>Agregar Estación</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Agregar Estación</DialogTitle>
          <DialogDescription>
            Complete los datos de la estación.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre</FormLabel>
                  <FormControl>
                    <Input {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <div className="flex gap-4">
              <FormField
                control={form.control}
                name="x"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Coordenada X</FormLabel>
                    <FormControl>
                      <Input type="number" {...field} required />
                    </FormControl>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="y"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Coordenada Y</FormLabel>
                    <FormControl>
                      <Input type="number" {...field} required />
                    </FormControl>
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="maxCapacity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Capacidad Máxima</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="mainStation"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>¿Es principal?</FormLabel>
                  <FormControl>
                    <RadioGroup
                      value={field.value ? 'true' : 'false'}
                      onValueChange={(val) => field.onChange(val === 'true')}
                      className="flex flex-row gap-4"
                    >
                      <FormItem className="flex items-center space-x-2">
                        <FormControl>
                          <RadioGroupItem value="true" id="mainStation-yes" />
                        </FormControl>
                        <FormLabel htmlFor="mainStation-yes">Sí</FormLabel>
                      </FormItem>
                      <FormItem className="flex items-center space-x-2">
                        <FormControl>
                          <RadioGroupItem value="false" id="mainStation-no" />
                        </FormControl>
                        <FormLabel htmlFor="mainStation-no">No</FormLabel>
                      </FormItem>
                    </RadioGroup>
                  </FormControl>
                  <FormDescription>
                    Indica si la estación es la principal.
                  </FormDescription>
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="submit" disabled>
                Guardar
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
