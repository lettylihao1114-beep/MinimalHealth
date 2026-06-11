import { registerPlugin } from '@capacitor/core'

export interface NativeBridgePlugin {
  openAbout(): Promise<void>
}

export const NativeBridge = registerPlugin<NativeBridgePlugin>('NativeBridge', {
  web: () => ({
    async openAbout() {
      // Web fallback: no-op
      console.log('NativeBridge.openAbout not available on web')
    },
  }),
})