<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true">
      <div class="login-bg__grid" />
      <div
        class="login-bg__image"
        :style="{ backgroundImage: `url(${loginBgUrl})` }"
      />
      <div class="login-bg__orb login-bg__orb--tl" />
      <div class="login-bg__orb login-bg__orb--br" />
      <div class="login-bg__scan" />
    </div>

    <main class="login-main">
      <header class="login-brand">
        <div class="login-brand__row">
          <el-icon class="login-brand__icon" :size="40">
            <Connection />
          </el-icon>
          <h1 class="login-brand__title">{{ title }}</h1>
        </div>
        <p class="login-brand__subtitle">智能问数 · 系统登录</p>
      </header>

      <div class="login-card glass-panel">
        <div class="login-card__accent login-card__accent--tl" />
        <div class="login-card__accent login-card__accent--br" />

        <el-form
          ref="loginRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          label-position="top"
          :hide-required-asterisk="true"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username" class="login-field">
            <template #label>
              <span class="field-label">账号</span>
            </template>
            <el-input
              v-model="loginForm.username"
              type="text"
              size="large"
              auto-complete="username"
              placeholder="请输入账号"
              class="login-input"
            >
              <template #prefix>
                <el-icon class="login-input__prefix-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password" class="login-field">
            <template #label>
              <span class="field-label">密码</span>
            </template>
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              auto-complete="current-password"
              placeholder="请输入密码"
              class="login-input"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon class="login-input__prefix-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="captchaEnabled" prop="code" class="login-field">
            <template #label>
              <span class="field-label">验证码</span>
            </template>
            <div class="login-captcha-row">
              <el-input
                v-model="loginForm.code"
                size="large"
                auto-complete="off"
                placeholder="验证码"
                class="login-input login-input--captcha"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon class="login-input__prefix-icon"><Key /></el-icon>
                </template>
              </el-input>
              <div class="login-captcha-img-wrap" @click="getCode">
                <img :src="codeUrl" alt="验证码" class="login-captcha-img" />
                <div class="login-captcha-img__overlay" />
              </div>
              <el-button
                class="login-captcha-refresh"
                text
                type="primary"
                :icon="Refresh"
                circle
                @click="getCode"
              />
            </div>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe" class="login-remember">
              记住密码
            </el-checkbox>
          </div>

          <el-form-item class="login-submit-wrap">
            <el-button
              :loading="loading"
              type="primary"
              class="login-submit"
              native-type="submit"
              @click.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
              <el-icon v-if="!loading" class="login-submit__arrow"><Right /></el-icon>
            </el-button>
            <div v-if="register" class="login-register">
              <router-link class="login-link" :to="'/register'">立即注册</router-link>
            </div>
          </el-form-item>
        </el-form>

        <div class="login-card__footer">
          <span class="login-muted">安全访问</span>
          <span class="login-muted-sep">|</span>
          <span class="login-muted">企业内网环境</span>
        </div>
      </div>

      <footer class="login-status">
        <div class="login-status__row">
          <div class="login-status__item">
            <span class="login-status__dot" />
            <span class="login-status__text">系统在线</span>
          </div>
          <div class="login-status__divider" />
          <span class="login-status__text">智能问数平台</span>
        </div>
        <p class="login-status__copy">{{ footerContent }}</p>
      </footer>
    </main>

    <div class="login-decor login-decor--tr" aria-hidden="true" />
    <div class="login-decor login-decor--bl" aria-hidden="true" />
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from "@/store/modules/user"
import defaultSettings from "@/settings"
import {
  Connection,
  User,
  Lock,
  Key,
  Refresh,
  Right
} from "@element-plus/icons-vue"

import loginBgUrl from "@/assets/images/login-bg-neural.jpg"

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(false)

const loginRules = computed(() => {
  const rules = {
    username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
    password: [{ required: true, trigger: "blur", message: "请输入您的密码" }]
  }
  if (captchaEnabled.value) {
    rules.code = [{ required: true, trigger: "change", message: "请输入验证码" }]
  }
  return rules
})
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    ...loginForm.value,
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang="scss" scoped>
@import url("https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@600;700&family=Inter:wght@400;500;600&display=swap");

/* 设计令牌 — 对齐 login_screen_modernized 深色科技风格 */
$bg-lowest: #0a0e17;
$surface: #0f131c;
$on-surface: #dfe2ef;
$on-variant: #b9cacb;
$outline-variant: #3a494b;
$primary-container: #00f2ff;
$on-primary-container: #006a71;
$secondary-container: #7000ff;
$glass: rgba(49, 53, 63, 0.4);

.login-page {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: $bg-lowest;
  color: $on-surface;
  font-family: "Inter", system-ui, sans-serif;
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.login-bg__grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(
    circle at 2px 2px,
    rgba(58, 73, 75, 0.15) 1px,
    transparent 0
  );
  background-size: 32px 32px;
}

.login-bg__image {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-size: cover;
  background-position: center;
}

.login-bg__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
}

.login-bg__orb--tl {
  top: -20%;
  left: -10%;
  width: 60%;
  height: 60%;
  background: rgba($secondary-container, 0.1);
}

.login-bg__orb--br {
  bottom: -10%;
  right: -10%;
  width: 50%;
  height: 50%;
  background: rgba($primary-container, 0.08);
}

.login-bg__scan {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(255, 255, 255, 0.02),
    transparent
  );
  height: 4px;
  width: 100%;
  animation: login-scan 8s linear infinite;
}

@keyframes login-scan {
  0% {
    transform: translateY(-100%);
  }
  100% {
    transform: translateY(100vh);
  }
}

/* 与 UI 稿一致：Tailwind max-w-md + px-6；大屏按约 32% 视口宽放大卡片（截图中卡片约占屏宽 30%–35%） */
.login-main {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: min(92vw, max(28rem, min(40rem, 32vw)));
  padding: 1.5rem;
  box-sizing: border-box;
}

.login-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 2.5rem; /* mb-10 */
  text-align: center;
}

.login-brand__row {
  display: flex;
  align-items: center;
  gap: 0.75rem; /* gap-3 */
  margin-bottom: 0.5rem; /* mb-2 */
}

.login-brand__icon {
  color: $primary-container;
  flex-shrink: 0;
}

/* text-3xl */
.login-brand__title {
  margin: 0;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-weight: 800;
  font-size: 1.875rem;
  letter-spacing: -0.03em;
  text-transform: uppercase;
  color: $primary-container;
  line-height: 1.2;
}

/* text-sm */
.login-brand__subtitle {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: $on-variant;
}

/* p-8 */
.glass-panel {
  background: $glass;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba($outline-variant, 0.35);
  padding: 2rem;
  position: relative;
  box-sizing: border-box;
}

.login-card__accent {
  position: absolute;
  width: 1rem;
  height: 1rem;
  pointer-events: none;
}

.login-card__accent--tl {
  top: 0;
  left: 0;
  border-top: 2px solid $primary-container;
  border-left: 2px solid $primary-container;
}

.login-card__accent--br {
  bottom: 0;
  right: 0;
  border-bottom: 2px solid $secondary-container;
  border-right: 2px solid $secondary-container;
}

/* space-y-6：表单项间距 1.5rem */
.login-form {
  :deep(.el-form-item) {
    margin-bottom: 1.5rem;
  }

  :deep(.el-form-item__label) {
    padding: 0;
    margin-bottom: 0.25rem;
    height: auto;
    line-height: 1.3;
  }
}

/* text-xs */
.field-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: $primary-container;
  margin-left: 0.125rem;
}

.login-input {
  :deep(.el-input__wrapper) {
    background: transparent;
    border: none;
    border-radius: 0;
    box-shadow: none;
    border-bottom: 1px solid rgba($outline-variant, 0.45);
    padding-left: 0.25rem;
    min-height: 3rem; /* py-3 量级，与设计稿输入高度一致 */
    padding-top: 0.75rem;
    padding-bottom: 0.75rem;
    transition: border-color 0.2s ease;
  }

  :deep(.el-input__wrapper:hover) {
    border-bottom-color: rgba($primary-container, 0.45);
  }

  :deep(.el-input__wrapper.is-focus) {
    border-bottom-color: $primary-container;
    box-shadow: none;
  }

  :deep(.el-input__inner) {
    color: $on-surface;
    font-family: "Inter", sans-serif;
    font-size: 1rem;
    line-height: 1.5;

    &::placeholder {
      color: rgba($on-variant, 0.45);
    }
  }

  :deep(.el-input__prefix) {
    margin-right: 0.5rem;
  }
}

/* text-lg */
.login-input__prefix-icon {
  color: #849495;
  font-size: 1.125rem;
  transition: color 0.2s ease;
}

.login-input :deep(.el-input__wrapper.is-focus) .login-input__prefix-icon {
  color: $primary-container;
}

.login-captcha-row {
  display: flex;
  align-items: center;
  gap: 1rem; /* gap-4 */
  width: 100%;
}

.login-input--captcha {
  flex: 1;
  min-width: 0;
}

.login-captcha-img-wrap {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem 0.75rem;
  background: rgba(28, 31, 41, 0.9);
  border: 1px solid rgba($outline-variant, 0.35);
  cursor: pointer;
  overflow: hidden;
}

/* h-8 w-24 */
.login-captcha-img {
  height: 2rem;
  width: 6rem;
  object-fit: cover;
  opacity: 0.85;
  filter: invert(1) grayscale(1) contrast(1.35);
}

.login-captcha-img__overlay {
  position: absolute;
  inset: 0;
  background: rgba($primary-container, 0.08);
  mix-blend-mode: overlay;
  pointer-events: none;
}

.login-captcha-refresh {
  flex-shrink: 0;
  color: rgba($on-variant, 0.85) !important;

  &:hover {
    color: $primary-container !important;
  }
}

.login-options {
  margin-bottom: 0.5rem;
}

.login-remember {
  :deep(.el-checkbox__label) {
    color: $on-variant;
    font-size: 0.8125rem;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: $primary-container;
    border-color: $primary-container;
  }

  :deep(.el-checkbox__inner) {
    border-color: rgba($outline-variant, 0.8);
    background: transparent;
  }
}

/* 设计稿主按钮前有 pt-4；此处为「记住密码」后的主按钮区 */
.login-submit-wrap {
  margin-bottom: 0 !important;
  padding-top: 1rem;

  :deep(.el-form-item__content) {
    display: block;
  }
}

/* py-4 */
.login-submit {
  width: 100%;
  height: auto !important;
  padding: 1rem 1.25rem !important;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-weight: 700;
  font-size: 1rem;
  letter-spacing: -0.02em;
  border: none !important;
  border-radius: 0 !important;
  background: $primary-container !important;
  color: $on-primary-container !important;
  box-shadow: 0 0 20px rgba(0, 242, 255, 0.22);
  transition: background 0.2s ease, transform 0.15s ease;

  &:hover {
    background: #00dbe7 !important;
    color: $on-primary-container !important;
  }

  &:active {
    transform: scale(0.98);
  }
}

.login-submit__arrow {
  margin-left: 0.35rem;
  vertical-align: middle;
}

.login-register {
  margin-top: 0.75rem;
  text-align: right;
}

.login-link {
  color: $on-variant;
  font-size: 0.8125rem;
  text-decoration: none;
  transition: color 0.2s ease;

  &:hover {
    color: $primary-container;
  }
}

/* mt-8 */
.login-card__footer {
  margin-top: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  font-size: 0.625rem; /* text-[10px] ≈ 10px */
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #849495;
}

.login-muted-sep {
  opacity: 0.5;
}

/* mt-12 */
.login-status {
  margin-top: 3rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  opacity: 0.65;
}

.login-status__row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.login-status__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.login-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $primary-container;
  box-shadow: 0 0 8px rgba(0, 242, 255, 0.75);
}

.login-status__text {
  font-size: 0.625rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: $on-variant;
}

.login-status__divider {
  width: 1px;
  height: 12px;
  background: rgba($outline-variant, 0.45);
}

.login-status__copy {
  margin: 0;
  font-size: 0.5625rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(132, 148, 149, 0.55);
  text-align: center;
  max-width: 100%;
}

.login-decor {
  position: absolute;
  pointer-events: none;
  opacity: 0.35;
  z-index: 1;
}

.login-decor--tr {
  top: 2.5rem;
  right: 2.5rem;
  width: 6rem;
  height: 1px;
  background: linear-gradient(to right, transparent, $primary-container);
}

.login-decor--tr::after {
  content: "";
  position: absolute;
  top: 0.75rem;
  right: 0;
  width: 4rem;
  height: 1px;
  background: linear-gradient(to right, transparent, $secondary-container);
}

.login-decor--bl {
  bottom: 2.5rem;
  left: 2.5rem;
  width: 4rem;
  height: 1px;
  background: linear-gradient(to left, transparent, $primary-container);
}

.login-decor--bl::after {
  content: "";
  position: absolute;
  top: 0.75rem;
  left: 0;
  width: 6rem;
  height: 1px;
  background: linear-gradient(to left, transparent, $secondary-container);
}
</style>
