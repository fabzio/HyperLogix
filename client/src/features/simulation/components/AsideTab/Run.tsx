import Typography from '@/components/typography'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { zodResolver } from '@hookform/resolvers/zod'
import { addDays, addMonths, addWeeks, addYears, format } from 'date-fns'
import { CalendarIcon, Loader2, PauseIcon, Square } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  useStartSimulation,
  useStatusSimulation,
  useStopSimulation,
} from '../../hooks/useSimulation'

export default function Run() {
  const { mutate: startSimulation, isPending } = useStartSimulation()
  const { mutate: stopSimulation } = useStopSimulation()
  const { data } = useStatusSimulation()
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      mode: 'absolute',
      absolute: {
        from: new Date('2025-01-01'),
        to: new Date('2025-01-08'),
      },
      relative: {
        startDate: new Date('2025-01-01'),
        duration: 1,
        unit: 'weeks',
      },
    },
  })

  const onSubmit = form.handleSubmit((data) => {
    let startDate: Date
    let endDate: Date = new Date()

    if (data.mode === 'absolute') {
      startDate = data.absolute.from
      endDate = data.absolute.to
    } else {
      startDate = data.relative.startDate
      const { duration, unit } = data.relative
      if (unit === 'days') {
        endDate = addDays(startDate, duration)
      } else if (unit === 'weeks') {
        endDate = addWeeks(startDate, duration)
      } else if (unit === 'months') {
        endDate = addMonths(startDate, duration)
      } else if (unit === 'years') {
        endDate = addYears(startDate, duration)
      }
    }

    startSimulation({
      startTimeOrders: startDate.toISOString(),
      endTimeOrders: endDate.toISOString(),
    })
  })

  const isRunning = data?.running || false

  const handleStop = () => {
    stopSimulation()
  }

  return (
    <article>
      <Typography variant="h3">Simulación</Typography>
      <Form {...form}>
        <form onSubmit={onSubmit}>
          <Tabs
            defaultValue="relative"
            onValueChange={(value) => {
              form.setValue('mode', value as 'absolute' | 'relative')
            }}
          >
            <TabsList>
              <TabsTrigger value="relative" className="w-full">
                Relativo
              </TabsTrigger>
              <TabsTrigger value="absolute" className="w-full">
                Absoluto
              </TabsTrigger>
            </TabsList>
            <TabsContent value="absolute">
              <FormField
                control={form.control}
                name="absolute"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Rango de simulación</FormLabel>
                    <Popover>
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant="outline"
                            disabled={isRunning}
                            className={cn(
                              'w-[300px] justify-start text-left font-normal',
                              !field.value && 'text-muted-foreground',
                            )}
                          >
                            <CalendarIcon />
                            {field.value?.from ? (
                              field.value.to ? (
                                <>
                                  {format(field.value.from, 'LLL dd, y')} -{' '}
                                  {format(field.value.to, 'LLL dd, y')}
                                </>
                              ) : (
                                format(field.value.from, 'LLL dd, y')
                              )
                            ) : (
                              <span>Elija el rango</span>
                            )}
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0" align="start">
                        <Calendar
                          mode="range"
                          defaultMonth={field.value?.from}
                          selected={field.value}
                          onSelect={field.onChange}
                          numberOfMonths={2}
                          disabled={isRunning}
                        />
                      </PopoverContent>
                    </Popover>
                  </FormItem>
                )}
              />
            </TabsContent>
            <TabsContent value="relative" className="space-y-4">
              <FormField
                control={form.control}
                name="relative.startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Fecha de inicio</FormLabel>
                    <Popover>
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant="outline"
                            disabled={isRunning}
                            className={cn(
                              'w-[300px] justify-start text-left font-normal',
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
                          initialFocus
                          mode="single"
                          selected={field.value}
                          onSelect={field.onChange}
                          disabled={isRunning}
                        />
                      </PopoverContent>
                    </Popover>
                  </FormItem>
                )}
              />
              <div className="flex gap-2">
                <FormField
                  control={form.control}
                  name="relative.duration"
                  render={({ field }) => (
                    <FormItem className="flex-1">
                      <FormLabel>Duración</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min={1}
                          disabled={isRunning}
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number.parseInt(e.target.value) || 1)
                          }
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="relative.unit"
                  render={({ field }) => (
                    <FormItem className="flex-1">
                      <FormLabel>Unidad</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                        disabled={isRunning}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Seleccione unidad" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="days">Días</SelectItem>
                          <SelectItem value="weeks">Semanas</SelectItem>
                          <SelectItem value="months">Meses</SelectItem>
                          <SelectItem value="years">Años</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormItem>
                  )}
                />
              </div>
            </TabsContent>
          </Tabs>
          <div className="mt-4">
            {isRunning ? (
              <div className="flex items-center justify-center gap-2">
                <Button size="icon" variant="secondary">
                  <PauseIcon className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  size="icon"
                  variant="destructive"
                  onClick={handleStop}
                >
                  <Square className="h-4 w-4" />
                </Button>
              </div>
            ) : (
              <Button type="submit" className="w-full" disabled={isPending}>
                {isPending ? (
                  <Loader2 className="animate-spin" />
                ) : (
                  'Iniciar simulación'
                )}
              </Button>
            )}
          </div>
        </form>
      </Form>
    </article>
  )
}

const formSchema = z.object({
  mode: z.enum(['absolute', 'relative']),
  absolute: z.object({
    from: z.date(),
    to: z.date(),
  }),
  relative: z.object({
    startDate: z.date(),
    duration: z.number().min(1, 'La duración debe ser mayor a 0'),
    unit: z.enum(['days', 'weeks', 'months', 'years']),
  }),
})
type FormSchema = z.infer<typeof formSchema>
