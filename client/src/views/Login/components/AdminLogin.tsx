import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { useSessionStore } from '@/store/session'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from '@tanstack/react-router'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

export default function AdminLogin() {
  const navigate = useNavigate()
  const { setUsername } = useSessionStore()
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
  })
  const onSubmit = form.handleSubmit((data) => {
    setUsername(data.username)
    navigate({ to: '/' })
  })
  return (
    <Card>
      <CardHeader>
        <CardTitle>Iniciar Sesión</CardTitle>
        <CardDescription>
          Gestione la operación diaria, simule y administre su flota de manera
          eficiente.
        </CardDescription>
      </CardHeader>
      <Form {...form}>
        <form onSubmit={onSubmit}>
          <CardContent className="space-y-2">
            <FormField
              control={form.control}
              name="username"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre de Usuario</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
          <CardFooter className="flex justify-center mt-2">
            <Button type="submit">Ingresar</Button>
          </CardFooter>
        </form>
      </Form>
    </Card>
  )
}

const formSchema = z.object({
  username: z.string({
    required_error: 'El nombre de usuario es requerido',
  }),
})
type FormSchema = z.infer<typeof formSchema>
