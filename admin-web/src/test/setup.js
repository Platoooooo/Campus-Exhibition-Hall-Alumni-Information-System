// vitest 全局 setup：模拟浏览器 API（jsdom 缺失部分）
import { vi } from 'vitest'

// mock IntersectionObserver
global.IntersectionObserver = vi.fn(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}))

// mock matchMedia
global.matchMedia = vi.fn(query => ({
  matches: false,
  media: query,
  onchange: null,
  addListener: vi.fn(),
  removeListener: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  dispatchEvent: vi.fn(),
}))

// mock getComputedStyle for jsdom
const origGetComputedStyle = global.getComputedStyle
global.getComputedStyle = (el, pseudo) => {
  const style = origGetComputedStyle ? origGetComputedStyle(el, pseudo) : {}
  return new Proxy(style, {
    get(target, prop) {
      return prop in target ? target[prop] : ''
    }
  })
}
