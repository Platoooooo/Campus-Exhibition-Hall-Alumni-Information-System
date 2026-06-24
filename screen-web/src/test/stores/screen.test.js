import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useScreenStore } from '@/stores/screen'

describe('useScreenStore 大屏状态', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('初始状态：idle，无校友', () => {
    const store = useScreenStore()
    expect(store.faceState).toBe('idle')
    expect(store.currentAlumni).toBeNull()
    expect(store.isFaceMode).toBe(false)
    expect(store.hitVersion).toBe(0)
  })

  it('onFaceHit 设置为 hit 状态并递增版本号', () => {
    const store = useScreenStore()
    const alumni = { id: 1, name: '张三', collegeName: '计算机学院' }
    const timeline = [{ archiveId: 1, title: 'ACM金牌' }]

    store.onFaceHit(alumni, timeline)

    expect(store.faceState).toBe('hit')
    expect(store.currentAlumni).toEqual(alumni)
    expect(store.isFaceMode).toBe(true)
    expect(store.hitVersion).toBe(1)
    expect(store.hitTimeline).toEqual(timeline)

    // 再次命中：版本号递增
    store.onFaceHit({ id: 2, name: '李四' }, [])
    expect(store.hitVersion).toBe(2)
  })

  it('onFaceMiss 更新状态为 miss', () => {
    const store = useScreenStore()
    store.onFaceMiss('miss')
    expect(store.faceState).toBe('miss')
  })

  it('onFaceMiss 更新状态为 degraded', () => {
    const store = useScreenStore()
    store.onFaceMiss('degraded')
    expect(store.faceState).toBe('degraded')
  })

  it('resetFace 恢复为 idle 并清除数据', () => {
    const store = useScreenStore()

    // 先设置命中状态
    store.onFaceHit({ id: 1, name: '张三' }, [{ title: 'test' }])
    expect(store.faceState).toBe('hit')

    store.resetFace()
    expect(store.faceState).toBe('idle')
    expect(store.hitAlumni).toBeNull()
    expect(store.hitTimeline).toEqual([])
    expect(store.currentAlumni).toBeNull()
    expect(store.isFaceMode).toBe(false)
  })

  it('setCarouselData 写入缓存并更新数据', () => {
    const store = useScreenStore()
    const carouselData = { name: '默认方案', items: [{ id: 1 }] }

    store.setCarouselData(carouselData)

    expect(store.carouselData).toEqual(carouselData)
    expect(store.isOffline).toBe(false)

    const cached = JSON.parse(localStorage.getItem('screen_cache_carousel'))
    expect(cached).toEqual(carouselData)
  })

  it('loadFromCache 从 localStorage 恢复轮播数据', () => {
    const cached = { name: '缓存方案', items: [] }
    localStorage.setItem('screen_cache_carousel', JSON.stringify(cached))

    const store = useScreenStore()
    const result = store.loadFromCache()

    expect(result).toBe(true)
    expect(store.carouselData).toEqual(cached)
    expect(store.isOffline).toBe(true)
  })

  it('loadFromCache 无缓存返回 false', () => {
    const store = useScreenStore()
    const result = store.loadFromCache()
    expect(result).toBe(false)
  })

  it('resetToDefault 关闭人脸模式', () => {
    const store = useScreenStore()
    store.setAlumni({ id: 1, name: '张三' })
    expect(store.isFaceMode).toBe(true)

    store.resetToDefault()
    expect(store.isFaceMode).toBe(false)
    expect(store.currentAlumni).toBeNull()
  })
})
