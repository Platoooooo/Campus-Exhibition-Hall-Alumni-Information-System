<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.loginAction(form.username, form.password)
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch {
    // 错误消息已在 request 拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="login-left">
      <div class="brand-content">
        <div class="brand-badge-wrap">
          <img src="@/assets/images/school_badge.png" class="brand-badge" alt="福软校徽" />
        </div>
        <h1 class="brand-title">福软校友展览馆</h1>
        <p class="brand-subtitle">校友资料管理系统</p>
        <div class="brand-features">
          <span>校友档案数字化</span>
          <span>人脸识别互动</span>
          <span>荣誉轨迹展示</span>
        </div>
      </div>
      <div class="brand-footer">Copyright © 2026 福州软件职业技术学院 陈灿</div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="login-right">
      <div class="login-card">
        <h2 class="card-title">账号登录</h2>
        <p class="card-desc">请输入您的账号与密码</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="账号"
              :prefix-icon="'User'"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="'Lock'"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              style="width: 100%"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中…' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---- 左侧品牌 ---- */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a3fb8 0%, #2B5AED 50%, #4f7af5 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.brand-content {
  text-align: center;
  color: #fff;
}

.brand-badge-wrap {
  width: 100px; height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    transparent 70%
  );
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
}

.brand-badge {
  width: 70px; height: 70px;
  object-fit: contain;
}

.brand-title {
  font-family: 'HarmonyOS_SansSC_Bold', 'Microsoft YaHei', sans-serif;
  font-size: 32px;
  margin: 0 0 8px;
  letter-spacing: 4px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.8;
  margin: 0 0 40px;
}

.brand-features {
  display: flex;
  gap: 24px;
  opacity: 0.75;
  font-size: 13px;
}

.brand-features span {
  padding: 6px 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
}

.brand-footer {
  position: absolute;
  bottom: 24px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

/* ---- 右侧登录 ---- */
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  flex-shrink: 0;
}

.login-card {
  width: 360px;
}

.card-title {
  font-family: 'HarmonyOS_SansSC_Bold', 'Microsoft YaHei', sans-serif;
  font-size: 24px;
  color: #1D2129;
  margin: 0 0 8px;
}

.card-desc {
  font-size: 14px;
  color: #86909C;
  margin: 0 0 32px;
}

/* Element Plus 图标通过 <el-icon> 传递时自定义前缀的样式适配 */
:deep(.el-input__prefix) {
  color: #86909C;
}
</style>
