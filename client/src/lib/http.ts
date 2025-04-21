import { env } from '@/env'
import axios from 'axios'

const http = axios.create({
  baseURL: env.VITE_API,
})

export default http
