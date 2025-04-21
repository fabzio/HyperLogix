import { act } from 'react'
import { describe, expect, it } from 'vitest'
import setup from './setup'

describe('Router setup', () => {
  it('should render the RouterProvider without crashing', async () => {
    const { app, router } = await setup()
    await act(() => {
      router.navigate({
        to: '/',
      })
    })
    expect(app).toBeTruthy()
  })
})
