import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useNavigate } from '@tanstack/react-router'

export default function AdminLogin() {
  const navigate = useNavigate()
  return (
    <Card>
      <CardHeader>
        <CardTitle>Iniciar Sesión</CardTitle>
        <CardDescription>
          Gestione la operación diaria, simule y administre su flota de manera
          eficiente.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-2">
        <div className="space-y-1">
          <Label htmlFor="username">Nombre de Usuario</Label>
          <Input id="username" />
        </div>
      </CardContent>
      <CardFooter className="flex justify-center">
        <Button
          onClick={() => {
            navigate({ to: '/' })
          }}
        >
          Ingresar
        </Button>
      </CardFooter>
    </Card>
  )
}
