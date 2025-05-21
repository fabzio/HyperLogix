import http from '@/lib/http'
import { useQuery } from '@tanstack/react-query'
import { createFileRoute } from '@tanstack/react-router'
import logo from '../logo.svg'

export const Route = createFileRoute('/start')({
  component: App,
})

function App() {
  const { data, refetch } = useQuery({
    enabled: false,
    queryKey: ['test'],
    queryFn: async () => {
      const response = await http.get('/')
      return response.data
    },
  })
  return (
    <div className="text-center">
      <header className="min-h-screen flex flex-col items-center justify-center bg-[#282c34] text-white text-[calc(10px+2vmin)]">
        <img
          src={logo}
          className="h-[40vmin] pointer-events-none animate-[spin_20s_linear_infinite]"
          alt="logo"
        />
        <div>
          <button
            type="button"
            onClick={() => refetch()}
            className="bg-[#61dafb] text-black font-bold py-2 px-4 rounded cursor-pointer"
          >
            Test Spring Boot API
          </button>
          <p>API Response: {data}</p>
          {data && (
            <div className="flex justify-center">
              <p className="bg-clip-text text-transparent bg-gradient-to-r from-purple-400 via-pink-500 to-red-500">
                It works!!!
              </p>
              <span>😁</span>
            </div>
          )}
        </div>
        <p>
          Edit <code>src/routes/index.tsx</code> and save to reload.
        </p>
        <a
          className="text-[#61dafb] hover:underline"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
        <a
          className="text-[#61dafb] hover:underline"
          href="https://tanstack.com"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn TanStack
        </a>
      </header>
    </div>
  )
}
