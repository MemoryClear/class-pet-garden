<template>
  <div class="support-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h2>💖 赞赏支持</h2>
    </div>

    <div class="content">
      <section class="intro">
        <h3>支持项目持续维护与更新</h3>
        <p v-if="systemName" class="system-name">——《{{ systemName }}》</p>
      </section>

      <section class="quote-card">
        <div class="quote-mark">"</div>
        <p class="quote-text">
          如果它替您省下了一些时间，真心希望您愿意支持一次。
        </p>
        <p class="quote-sub">
          这个项目没有广告、会员或强制付费。持续维护、bug 修复、功能开发与服务器支出，都来自每一位用户的支持。
        </p>
        <div class="tags">
          <span class="tag">☕ 服务器运行</span>
          <span class="tag">🔧 持续维护</span>
          <span class="tag">✨ 功能开发</span>
        </div>
      </section>

      <section class="qr-card">
        <div class="qr-header">
          <span class="qr-title">
            <span class="qr-icon">微</span>
            微信赞赏码
          </span>
          <span class="qr-hint">感谢您的认可与支持</span>
        </div>

        <div class="qr-body">
          <img
            v-if="qrUrl"
            :src="qrUrl"
            alt="微信赞赏码"
            class="qr-image"
            @click="showQr = true"
          />
          <div v-else class="qr-placeholder">
            <div class="qr-placeholder-icon">📱</div>
            <p>赞赏码图片待上传</p>
            <p class="qr-placeholder-hint">
              请将微信赞赏码放到 <code>backend/src/main/resources/static/support/wechat-qr.png</code>
            </p>
          </div>

          <p v-if="qrUrl" class="qr-footer">
            哪怕只是一点点支持，也会被认真地用于后续维护。点击图片可查看清晰原图。
          </p>
        </div>
      </section>

      <section class="footer">
        <p>感谢每一位愿意支持的人 ❤️</p>
      </section>
    </div>

    <!-- 放大查看 -->
    <div v-if="showQr" class="qr-modal" @click="showQr = false">
      <img :src="qrUrl" alt="微信赞赏码" class="qr-modal-image" />
      <p class="qr-modal-hint">点击任意处关闭</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app.js'

const router = useRouter()
const appStore = useAppStore()

const qrUrl = ref('/support/wechat-qr.png')
const showQr = ref(false)
const systemName = ref('')

onMounted(async () => {
  try {
    if (!appStore.settings) {
      await appStore.fetchSettings()
    }
    systemName.value = appStore.settings?.systemName || ''
  } catch (e) {
    // 静默失败
  }
})
</script>

<style scoped>
.support-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

.header-bar {
  background: #fff;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  background: #f0f0f0;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.header-bar h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.content {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.intro h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #666;
  font-weight: 500;
}

.system-name {
  color: #f97316;
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.quote-card {
  background: linear-gradient(135deg, #fff7ed 0%, #fef3c7 100%);
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 2px 12px rgba(249, 115, 22, 0.08);
  position: relative;
}

.quote-mark {
  position: absolute;
  top: 12px;
  left: 20px;
  font-size: 48px;
  color: #f97316;
  opacity: 0.2;
  font-family: Georgia, serif;
  line-height: 1;
}

.quote-text {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
  line-height: 1.5;
  position: relative;
}

.quote-sub {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 20px 0;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid #fed7aa;
  color: #c2410c;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.qr-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.qr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.qr-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.qr-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #07c160;
  color: #fff;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 700;
}

.qr-hint {
  font-size: 12px;
  color: #9ca3af;
}

.qr-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-image {
  max-width: 280px;
  width: 100%;
  height: auto;
  border-radius: 12px;
  cursor: zoom-in;
  background: #f9fafb;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.qr-placeholder {
  background: #f9fafb;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  color: #6b7280;
  width: 100%;
  max-width: 280px;
}

.qr-placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.qr-placeholder-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 12px;
  line-height: 1.5;
}

.qr-placeholder-hint code {
  background: #e5e7eb;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  word-break: break-all;
}

.qr-footer {
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  margin: 16px 0 0 0;
  line-height: 1.5;
}

.footer {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 20px 0;
}

.footer p {
  margin: 0;
}

.qr-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 100;
  cursor: zoom-out;
  padding: 20px;
}

.qr-modal-image {
  max-width: 90vw;
  max-height: 80vh;
  background: #fff;
  padding: 16px;
  border-radius: 12px;
}

.qr-modal-hint {
  color: #fff;
  font-size: 13px;
  margin-top: 16px;
  opacity: 0.7;
}

@media (max-width: 600px) {
  .content {
    padding: 0 12px;
  }

  .quote-card {
    padding: 20px 16px;
  }

  .quote-text {
    font-size: 17px;
  }

  .qr-card {
    padding: 16px;
  }
}
</style>
