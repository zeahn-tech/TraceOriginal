import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const servicesState = { firebaseReady: true, app: {} as unknown }
vi.mock('./services', () => ({
  get firebaseReady() { return servicesState.firebaseReady },
  get app() { return servicesState.app },
  subscribeToAlerts: vi.fn().mockResolvedValue({ ok: true }),
}))

const messagingMocks = {
  isSupported: vi.fn().mockResolvedValue(true),
  getMessaging: vi.fn(() => ({ __messaging: true })),
  getToken: vi.fn().mockResolvedValue('a-real-looking-fcm-token'),
  onMessage: vi.fn((_messaging: unknown, _handler: (payload: { notification?: { title?:string; body?:string } }) => void) => vi.fn()),
}
vi.mock('firebase/messaging', () => messagingMocks)

import { enablePushNotifications, listenForForegroundAlerts } from './push'
import { subscribeToAlerts } from './services'

describe('push notifications', () => {
  beforeEach(() => {
    servicesState.firebaseReady = true
    messagingMocks.isSupported.mockResolvedValue(true)
    messagingMocks.getToken.mockResolvedValue('a-real-looking-fcm-token')
    vi.stubGlobal('Notification', { requestPermission: vi.fn().mockResolvedValue('granted') })
    vi.stubGlobal('navigator', { ...navigator, serviceWorker: { ready: Promise.resolve({}) } })
    vi.stubEnv('VITE_FIREBASE_VAPID_KEY', 'a-fake-vapid-key')
    vi.clearAllMocks()
    messagingMocks.isSupported.mockResolvedValue(true)
    messagingMocks.getToken.mockResolvedValue('a-real-looking-fcm-token')
  })

  afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs() })

  it('[happy path] requests permission, gets a token, and subscribes it to the alerts topic', async () => {
    (Notification.requestPermission as ReturnType<typeof vi.fn>).mockResolvedValue('granted')
    const outcome = await enablePushNotifications()
    expect(outcome).toEqual({ ok: true })
    expect(subscribeToAlerts).toHaveBeenCalledWith({ token: 'a-real-looking-fcm-token' })
  })

  it('[permission declined] does not call subscribeToAlerts at all', async () => {
    (Notification.requestPermission as ReturnType<typeof vi.fn>).mockResolvedValue('denied')
    const outcome = await enablePushNotifications()
    expect(outcome).toEqual({ ok: false, reason: 'Notification permission was not granted.' })
    expect(subscribeToAlerts).not.toHaveBeenCalled()
  })

  it('[Firebase not configured] fails gracefully rather than throwing', async () => {
    servicesState.firebaseReady = false
    const outcome = await enablePushNotifications()
    expect(outcome.ok).toBe(false)
    expect(subscribeToAlerts).not.toHaveBeenCalled()
  })

  it('[browser lacks FCM support] fails gracefully rather than throwing', async () => {
    messagingMocks.isSupported.mockResolvedValue(false)
    const outcome = await enablePushNotifications()
    expect(outcome.ok).toBe(false)
    expect(subscribeToAlerts).not.toHaveBeenCalled()
  })

  it('[no VAPID key configured for this deployment] fails gracefully with a clear reason', async () => {
    vi.stubEnv('VITE_FIREBASE_VAPID_KEY', '')
    const outcome = await enablePushNotifications()
    expect(outcome).toEqual({ ok: false, reason: 'Push notifications are not configured for this deployment.' })
  })

  it('[foreground listener] forwards the FCM payload title/body to the callback', async () => {
    let capturedHandler: ((payload: { notification?: { title?:string; body?:string } }) => void) | undefined
    messagingMocks.onMessage.mockImplementation((_messaging, handler) => { capturedHandler = handler; return vi.fn() })
    const onAlert = vi.fn()
    listenForForegroundAlerts(onAlert)
    await vi.waitFor(() => { if (!capturedHandler) throw new Error('onMessage handler not yet registered') })
    capturedHandler?.({ notification: { title: 'Flood Warning', body: 'Rising water levels reported.' } })
    expect(onAlert).toHaveBeenCalledWith('Flood Warning', 'Rising water levels reported.')
  })

  it('[foreground listener, unconfigured] never throws when Firebase is not configured', async () => {
    servicesState.firebaseReady = false
    expect(() => listenForForegroundAlerts(vi.fn())).not.toThrow()
  })
})
