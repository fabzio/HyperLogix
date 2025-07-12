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
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { zodResolver } from '@hookform/resolvers/zod'
import { addDays, addMonths, addWeeks, addYears, format } from 'date-fns'
import {
  CalendarIcon,
  FastForward,
  Loader2,
  PauseIcon,
  Play,
  Rewind,
  Square,
} from 'lucide-react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  useCommandSimulation,
  useStartSimulation,
  useStatusSimulation,
  useStopSimulation,
} from '../../hooks/useSimulation'

export default function Run() {
  const { mutate: startSimulation, isPending } = useStartSimulation()
  const { mutate: sendCommand } = useCommandSimulation()
  const { mutate: stopSimulation } = useStopSimulation()
  const { data: status } = useStatusSimulation()
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      simulationType: 'simple',
      mode: 'relative',
      executionMode: 'simulation',
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

    if (data.executionMode === 'real') {
      startDate = new Date()
      endDate = addDays(startDate, 3)
    } else if (data.simulationType === 'collapse') {
      // For "Hasta el colapso": start today, end at end of week
      startDate = new Date()
      const today = new Date()
      const daysUntilSunday = 7 - today.getDay()
      endDate = addDays(today, daysUntilSunday === 7 ? 0 : daysUntilSunday)
    } else if (data.mode === 'absolute') {
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
      mode: data.executionMode,
    })
  })

  const isRunning = status?.running || false
  const executionMode = form.watch('executionMode')
  const simulationType = form.watch('simulationType')
  const isCollapseMode = simulationType === 'collapse'

  const handleStop = () => {
    stopSimulation()
  }
  const handlePause = () => {
    sendCommand({ command: 'PAUSE' })
  }
  const handleResume = () => {
    sendCommand({ command: 'RESUME' })
  }
  const handleDesaccelerate = () => {
    if (status?.timeAcceleration && status.timeAcceleration >= 1) {
      sendCommand({ command: 'DESACCELERATE' })
    }
  }
  const handleAccelerate = () => {
    if (status?.timeAcceleration && status.timeAcceleration <= 1024) {
      sendCommand({ command: 'ACCELERATE' })
    }
  }
  return (
    <article>
      <Typography variant="h3">Simulación</Typography>
      <Form {...form}>
        <form onSubmit={onSubmit}>
          <FormField
            control={form.control}
            name="simulationType"
            render={({ field }) => (
              <FormItem className="mb-4">
                <FormLabel>Tipo de simulación</FormLabel>
                <FormControl>
                  <RadioGroup
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                    disabled={isRunning}
                    className="flex flex-col space-y-2"
                  >
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="simple" id="simple" />
                      <Label htmlFor="simple">Simple</Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="collapse" id="collapse" />
                      <Label htmlFor="collapse">Hasta el colapso</Label>
                    </div>
                  </RadioGroup>
                </FormControl>
              </FormItem>
            )}
          />

          {isCollapseMode ? (
            <div className="space-y-4">
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
                          mode="single"
                          autoFocus
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
                          disabled={true}
                          value="Hasta el colapso"
                          readOnly
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
                        disabled={true}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="-" />
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
            </div>
          ) : (
            <Tabs
              value={form.watch('mode')}
              onValueChange={(value) => {
                form.setValue('mode', value as 'absolute' | 'relative')
              }}
            >
              <TabsList>
                <TabsTrigger
                  value="relative"
                  className="w-full"
                  disabled={isRunning}
                >
                  Relativo
                </TabsTrigger>
                <TabsTrigger
                  value="absolute"
                  className="w-full"
                  disabled={isRunning}
                >
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
                            mode="single"
                            autoFocus
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
                              field.onChange(
                                Number.parseInt(e.target.value) || 1,
                              )
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
          )}

          <div className="mt-4">
            {isRunning ? (
              <div className="flex items-center justify-center gap-2">
                {executionMode === 'simulation' && !isCollapseMode && (
                  <>
                    <Button
                      type="button"
                      size="icon"
                      variant="secondary"
                      onClick={handleDesaccelerate}
                      disabled={(status?.timeAcceleration ?? 1) <= 1}
                    >
                      <Rewind className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      size="icon"
                      variant="secondary"
                      onClick={status?.paused ? handleResume : handlePause}
                    >
                      {status?.paused ? (
                        <Play className="h-4 w-4" />
                      ) : (
                        <PauseIcon className="h-4 w-4" />
                      )}
                    </Button>
                  </>
                )}
                <Button
                  type="button"
                  size="icon"
                  variant="destructive"
                  onClick={handleStop}
                >
                  <Square className="h-4 w-4" />
                </Button>
                {executionMode === 'simulation' && !isCollapseMode && (
                  <Button
                    type="button"
                    size="icon"
                    variant="secondary"
                    onClick={handleAccelerate}
                    disabled={(status?.timeAcceleration ?? 1) >= 1024}
                  >
                    <FastForward className="h-4 w-4" />
                  </Button>
                )}
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                <Button
                  type="submit"
                  className="w-full"
                  disabled={isPending}
                  onClick={() => form.setValue('executionMode', 'simulation')}
                >
                  {isPending ? (
                    <Loader2 className="animate-spin" />
                  ) : (
                    'Iniciar simulación'
                  )}
                </Button>
              </div>
            )}
          </div>
        </form>
      </Form>
    </article>
  )
}

const formSchema = z.object({
  simulationType: z.enum(['simple', 'collapse']),
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
  executionMode: z.enum(['simulation', 'real']),
})
type FormSchema = z.infer<typeof formSchema>
