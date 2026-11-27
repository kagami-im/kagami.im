<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useArticlesStore } from '../stores/articles'
import { usePlayerStore } from '../stores/player'
import { renderMarkdown } from '../data/markdown'

const route = useRoute()
const router = useRouter()
const articlesStore = useArticlesStore()
const playerStore = usePlayerStore()
const { selectedArticle, articleBody, detailStatus } = storeToRefs(articlesStore)

const renderedHtml = computed(() => renderMarkdown(articleBody.value))
const readProgress = ref(0)

const updateProgress = () => {
  const doc = document.documentElement
  const scrollable = doc.scrollHeight - window.innerHeight
  if (scrollable <= 0) {
    readProgress.value = 0
    return
  }
  readProgress.value = Math.min(100, Math.max(0, (window.scrollY / scrollable) * 100))
}

watch(
  () => route.params.slug,
  (slugParam) => {
    const slug = Array.isArray(slugParam) ? slugParam[0] : slugParam
    if (typeof slug === 'string' && slug.length > 0) {
      void articlesStore.loadArticleBySlug(slug)
    } else {
      articlesStore.clearArticle()
    }
    readProgress.value = 0
  },
  { immediate: true }
)

onMounted(() => {
  updateProgress()
  window.addEventListener('scroll', updateProgress, { passive: true })
  window.addEventListener('resize', updateProgress, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateProgress)
  window.removeEventListener('resize', updateProgress)
  articlesStore.clearArticle()
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    void router.push({ name: 'home' })
  }
}
</script>

<template>
  <main class="detail-page" :class="{ veiled: playerStore.form === 'expanded' }">
    <div
      v-if="detailStatus === 'ready'"
      class="reading-progress"
      :style="{ width: `${readProgress}%` }"
      aria-hidden="true"
    ></div>

    <div class="detail-shell">
      <button type="button" class="back-button" @click="goBack" @dblclick.stop>← 返回</button>

      <article v-if="detailStatus === 'ready' && selectedArticle" class="detail-article selectable">
        <header class="detail-header">
          <div class="detail-meta">
            <span class="tabular-nums">{{ selectedArticle.publishedAt.slice(0, 10) }}</span>
            <span class="meta-sep">·</span>
            <span class="tabular-nums">约 {{ selectedArticle.readingMinutes }} 分钟阅读</span>
            <template v-if="selectedArticle.tags.length">
              <span class="meta-sep">·</span>
              <span class="meta-tags">{{ selectedArticle.tags.join(' / ') }}</span>
            </template>
          </div>
          <h1 class="detail-title">{{ selectedArticle.title }}</h1>
        </header>

        <!-- markdown-it runs with html:false; body comes from the article source. -->
        <div class="detail-body" v-html="renderedHtml"></div>
      </article>

      <section v-else-if="detailStatus === 'loading'" class="detail-state">
        <p class="state-text">文章加载中…</p>
      </section>

      <section v-else-if="detailStatus === 'not-found'" class="detail-state">
        <p class="state-text">未找到这篇文章。</p>
        <RouterLink to="/" class="state-action">返回首页</RouterLink>
      </section>

      <section v-else-if="detailStatus === 'error'" class="detail-state">
        <p class="state-text">文章加载失败，请稍后重试。</p>
        <button
          type="button"
          class="state-action"
          @click="articlesStore.loadArticleBySlug(String(route.params.slug ?? ''))"
        >
          重试
        </button>
      </section>
    </div>
  </main>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: calc(58px + var(--space-8)) var(--space-6) var(--space-16);
  transition:
    opacity var(--duration-slow) ease 0.06s,
    transform var(--duration-slow) var(--ease-out) 0.06s,
    filter var(--duration-slow) var(--ease-out) 0.06s;
}

.detail-page.veiled {
  opacity: 0;
  transform: translateY(14px);
  filter: blur(6px);
}

.reading-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--color-accent-warm);
  z-index: 25;
  pointer-events: none;
  transition: width 80ms linear;
}

.detail-shell {
  max-width: var(--layout-reading-width);
  margin-inline: auto;
}

.back-button {
  display: inline-flex;
  align-items: center;
  margin-bottom: var(--space-6);
  padding: 5px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface-glass);
  color: var(--color-text-muted);
  font-size: 12.5px;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    color var(--duration-fast) ease;
}

.back-button:hover {
  background: var(--color-surface-muted);
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

.back-button:focus-visible,
.state-action:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.detail-article {
  padding: var(--space-10) var(--space-10);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-soft);
  transition:
    background-color var(--duration-base) ease,
    border-color var(--duration-base) ease;
}

.detail-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-16) var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface-glass);
  text-align: center;
}

.state-text {
  color: var(--color-text-muted);
  font-size: 14px;
}

.state-action {
  padding: 5px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-text);
  font-size: 12.5px;
  cursor: pointer;
}

.state-action:hover {
  background: var(--color-surface-muted);
}

.detail-header {
  padding-bottom: var(--space-6);
  margin-bottom: var(--space-8);
  border-bottom: 1px solid var(--color-border);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-faint);
  font-size: 12px;
}

.meta-sep {
  opacity: 0.6;
}

.meta-tags {
  color: var(--color-primary);
}

.detail-title {
  margin-top: var(--space-3);
  font-family: var(--font-serif);
  font-size: clamp(22px, 3vw, 28px);
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0.01em;
  color: var(--color-text);
}

:deep(.detail-body) {
  color: var(--color-text);
  line-height: 1.85;
  font-size: 15.5px;
  word-break: break-word;
}

:deep(.detail-body h1) {
  margin-block: var(--space-10) var(--space-4);
  font-family: var(--font-serif);
  font-size: 22px;
  line-height: 1.4;
}

:deep(.detail-body h2) {
  margin-block: var(--space-8) var(--space-3);
  font-family: var(--font-serif);
  font-size: 19px;
  line-height: 1.45;
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

:deep(.detail-body h3) {
  margin-block: var(--space-6) var(--space-2);
  font-family: var(--font-serif);
  font-size: 16.5px;
  line-height: 1.45;
}

:deep(.detail-body h4) {
  margin-block: var(--space-5) var(--space-2);
  font-size: 15px;
  font-weight: 600;
}

:deep(.detail-body p) {
  margin-block: var(--space-4);
}

:deep(.detail-body ul),
:deep(.detail-body ol) {
  margin-block: var(--space-4);
  padding-left: var(--space-6);
}

:deep(.detail-body ul) {
  list-style: disc;
}

:deep(.detail-body ol) {
  list-style: decimal;
}

:deep(.detail-body li) {
  margin-block: var(--space-1);
}

:deep(.detail-body blockquote) {
  margin: var(--space-5) 0;
  padding: var(--space-3) var(--space-5);
  border-left: 2px solid var(--color-accent-warm);
  border-radius: 0 var(--radius-xs) var(--radius-xs) 0;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

:deep(.detail-body blockquote p) {
  margin-block: var(--space-1);
}

:deep(.detail-body code) {
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  background: var(--color-surface-muted);
  font-family: var(--font-mono);
  font-size: 0.88em;
}

:deep(.detail-body pre) {
  margin-block: var(--space-5);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  overflow-x: auto;
}

:deep(.detail-body pre code) {
  padding: 0;
  border-radius: 0;
  background: transparent;
  font-size: 13px;
  line-height: 1.65;
}

:deep(.detail-body img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin-block: var(--space-5);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

:deep(.detail-body hr) {
  margin-block: var(--space-8);
  border: 0;
  border-top: 1px solid var(--color-border);
}

:deep(.detail-body table) {
  width: 100%;
  margin-block: var(--space-5);
  border-collapse: collapse;
  font-size: 14px;
  display: block;
  overflow-x: auto;
}

:deep(.detail-body th),
:deep(.detail-body td) {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  text-align: left;
}

:deep(.detail-body th) {
  background: var(--color-surface-muted);
  font-weight: 600;
}

:deep(.detail-body a) {
  color: var(--color-primary);
  text-decoration: underline;
  text-underline-offset: 3px;
}

@media (max-width: 767px) {
  .detail-page {
    padding: calc(92px + var(--space-3)) var(--space-4) var(--space-12);
  }

  .detail-article {
    padding: var(--space-6) var(--space-5);
  }
}

@media (prefers-reduced-motion: reduce) {
  .detail-page {
    transition: opacity 0.2s ease;
  }

  .detail-page.veiled {
    transform: none;
    filter: none;
  }
}
</style>
