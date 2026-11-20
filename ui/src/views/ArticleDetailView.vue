<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useArticlesStore } from '../stores/articles'
import { renderMarkdown } from '../data/markdown'

const emit = defineEmits<{ back: [] }>()
const store = useArticlesStore()
const { selectedArticle, articleBody } = storeToRefs(store)
</script>

<template>
  <div class="detail-page">
    <button type="button" class="back-button" @click="emit('back')">← 返回</button>

    <article v-if="selectedArticle" class="detail-card">
      <header class="detail-header">
        <h1 class="detail-title">{{ selectedArticle.title }}</h1>
        <div class="detail-meta">
          <span>{{ selectedArticle.publishedAt.slice(0, 10) }}</span>
          <span v-if="selectedArticle.tags.length">{{ selectedArticle.tags.join(' · ') }}</span>
        </div>
      </header>

      <!-- markdown-it runs with html:false; body comes from a local markdown file. -->
      <div class="detail-body" v-html="renderMarkdown(articleBody)"></div>
    </article>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  /* Clear the fixed AppHeader (min-height 56px) so the back button stays visible. */
  padding: calc(56px + var(--space-4)) var(--space-4) var(--space-8);
}

.back-button {
  margin-bottom: var(--space-5);
  padding: var(--space-2) var(--space-4);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back-button:hover {
  color: var(--color-text);
}

.detail-card {
  max-width: 760px;
  margin-inline: auto;
  padding: var(--space-6) var(--space-8);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
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
  line-height: 1.75;
  font-size: 15px;
}

:deep(.detail-body h2) {
  margin-block: var(--space-6) var(--space-3);
  font-size: 19px;
}

:deep(.detail-body p) {
  margin-block: var(--space-3);
}

:deep(.detail-body pre) {
  padding: var(--space-4);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  overflow-x: auto;
}

:deep(.detail-body a) {
  color: var(--color-primary);
}

@media (max-width: 767px) {
  .detail-card {
    padding: var(--space-5);
  }
}
</style>
