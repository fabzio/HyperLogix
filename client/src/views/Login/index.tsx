import { GradientBackground } from '@/components/GradientBackground'
import Hyperlogix from '@/components/Hyperlogix'
import Logo from '@/components/Logo'
import { ModeToggle } from '@/components/mode-togle'
import Typography from '@/components/typography'
import CommitHash from '@/components/CommitHash'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import AdminLogin from './components/AdminLogin'
import DriverLogin from './components/DriverLogin'

export default function Login() {
  return (
    <div className="relative flex flex-col min-h-screen overflow-hidden">
      <GradientBackground />
      <header className="flex w-full justify-end px-4 py-1 relative z-10">
        <ModeToggle />
      </header>

      <div className="absolute bottom-4 left-4 z-10">
        <CommitHash />
      </div>

      <section className="flex grow items-center justify-center  z-10">
        <article className="flex flex-col gap-4 items-center">
          <Logo size={80} />
          <Typography variant="h1" className="flex items-center gap-2">
            <Hyperlogix />
          </Typography>
          <Tabs defaultValue="admin" className="w-[400px]">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="admin">Administrador</TabsTrigger>
              <TabsTrigger value="driver">Conductor</TabsTrigger>
            </TabsList>
            <TabsContent value="admin">
              <AdminLogin />
            </TabsContent>
            <TabsContent value="driver">
              <DriverLogin />
            </TabsContent>
          </Tabs>
        </article>
      </section>
    </div>
  )
}
