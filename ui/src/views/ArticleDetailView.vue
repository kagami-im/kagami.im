<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'
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

watch(
  () => route.params.slug,
  (slugParam) => {
    const slug = Array.isArray(slugParam) ? slugParam[0] : slugParam
    if (typeof slug === 'string' && slug.length > 0) {
      void articlesStore.loadArticleBySlug(slug)
    } else {
      articlesStore.clearArticle()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
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
    <div class="detail-shell">
      <button type="button" class="back-button" @click="goBack">← 返回</button>

      <article v-if="detailStatus === 'ready' && selectedArticle" class="detail-card selectable">
        <header class="detail-header">
          <h1 class="detail-title">{{ selectedArticle.title }}</h1>
          <div class="detail-meta">
            <span>{{ selectedArticle.publishedAt.slice(0, 10) }}</span>
            <span>约 {{ selectedArticle.readingMinutes }} 分钟阅读</span>
            <span v-if="selectedArticle.tags.length">{{ selectedArticle.tags.join(' · ') }}</span>
          </div>
        </header>

        <!-- markdown-it runs with html:false; body comes from the article source. -->
        <div class="detail-body" v-html="renderedHtml"></div>
      </article>

      <section v-else-if="detailStatus === 'loading'" class="detail-card detail-state">
        <p class="state-text">文章加载中…</p>
      </section>

      <section v-else-if="detailStatus === 'not-found'" class="detail-card detail-state">
        <p class="state-text">未找到这篇文章。</p>
        <RouterLink to="/" class="state-action">返回首页</RouterLink>
      </section>

      <section v-else-if="detailStatus === 'error'" class="detail-card detail-state">
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
  /* Clear the fixed AppHeader (min-height 56px) so the back button stays visible. */
  padding: calc(56px + var(--space-4)) var(--space-4) var(--space-8);
  transition:
    opacity 0.6s ease 0.08s,
    transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) 0.08s,
    filter 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) 0.08s;
}

.detail-page.veiled {
  opacity: 0;
  transform: translateY(14px);
  filter: blur(6px);
}

.detail-shell {
  max-width: 760px;
  margin-inline: auto;
}

.back-button {
  margin-bottom: var(--space-4);
  padding: var(--space-2) var(--space-4);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-text-muted);
  box-shadow: var(--shadow-soft);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back-button:hover {
  color: var(--color-text);
}

.back-button:focus-visible,
.state-action:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.detail-card {
  padding: var(--space-6) var(--space-8);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.detail-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding-block: var(--space-12);
  text-align: center;
}

.state-text {
  color: var(--color-text-muted);
  font-size: 14px;
}

.state-action {
  padding: var(--space-2) var(--space-4);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-nav-surface);
  color: var(--color-text);
  font-size: 13px;
  cursor: pointer;
}

.detail-header {
  padding-bottom: var(--space-4);
  margin-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.35;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-2);
  color: var(--color-text-faint);
  font-size: 12px;
}

:deep(.detail-body) {
  color: var(--color-text);
  line-height: 1.8;
  font-size: 15px;
  word-break: break-word;
}

:deep(.detail-body h1) {
  margin-block: var(--space-8) var(--space-4);
  font-size: 22px;
  line-height: 1.35;
}

:deep(.detail-body h2) {
  margin-block: var(--space-6) var(--space-3);
  font-size: 19px;
  line-height: 1.4;
}

:deep(.detail-body h3) {
  margin-block: var(--space-5) var(--space-2);
  font-size: 16px;
  line-height: 1.45;
}

:deep(.detail-body h4) {
  margin-block: var(--space-4) var(--space-2);
  font-size: 15px;
  font-weight: 600;
}

:deep(.detail-body p) {
  margin-block: var(--space-3);
}

:deep(.detail-body ul),
:deep(.detail-body ol) {
  margin-block: var(--space-3);
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
  margin: var(--space-4) 0;
  padding: var(--space-2) var(--space-4);
  border-left: 3px solid var(--color-primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

:deep(.detail-body blockquote p) {
  margin-block: var(--space-1);
}

:deep(.detail-body code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: var(--color-surface-muted);
  font-family: ui-monospace, 'Cascadia Code', 'Fira Code', Consolas, monospace;
  font-size: 0.9em;
}

:deep(.detail-body pre) {
  margin-block: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  overflow-x: auto;
}

:deep(.detail-body pre code) {
  padding: 0;
  border-radius: 0;
  background: transparent;
  font-size: 13px;
  line-height: 1.6;
}

:deep(.detail-body img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin-block: var(--space-4);
  border-radius: var(--radius-sm);
}

:deep(.detail-body hr) {
  margin-block: var(--space-6);
  border: 0;
  border-top: 1px solid var(--color-border);
}

:deep(.detail-body table) {
  width: 100%;
  margin-block: var(--space-4);
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
    /* Mobile AppHeader wraps to two rows; give a bit more top clearance. */
    padding: calc(88px + var(--space-3)) var(--space-3) var(--space-6);
  }

  .detail-card {
    padding: var(--space-5);
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
