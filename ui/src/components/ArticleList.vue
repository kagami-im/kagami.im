<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useArticlesStore } from '../stores/articles'

const store = useArticlesStore()
const { articles, listStatus } = storeToRefs(store)
</script>

<template>
  <section class="article-index" aria-label="文章目录" @dblclick.stop>
    <header class="section-heading">
      <div class="heading-group">
        <h2 class="section-title">杂记</h2>
        <span class="section-sub">Notes &amp; Essays</span>
      </div>
      <span v-if="listStatus === 'ready'" class="section-meta tabular-nums">
        {{ String(articles.length).padStart(2, '0') }} 篇
      </span>
    </header>

    <p v-if="listStatus === 'loading'" class="list-status">文章加载中…</p>

    <div v-else-if="listStatus === 'error'" class="list-status">
      <span>文章加载失败</span>
      <button type="button" class="retry-button" @click="store.loadArticles(true)">重试</button>
    </div>

    <p v-else-if="listStatus === 'ready' && articles.length === 0" class="list-status">
      什么都没有留下
    </p>

    <ol v-else class="article-rows">
      <li v-for="(article, i) in articles" :key="article.slug" class="article-item">
        <RouterLink
          :to="{ name: 'article-detail', params: { slug: article.slug } }"
          class="article-row"
          :aria-label="`阅读：${article.title}`"
        >
          <span class="article-num tabular-nums">{{ String(i + 1).padStart(2, '0') }}</span>

          <div class="article-body">
            <div class="article-head">
              <h3 class="article-title">{{ article.title }}</h3>
              <span class="article-date tabular-nums">{{ article.publishedAt.slice(0, 10) }}</span>
            </div>
            <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
            <div v-if="article.tags.length || article.pinned" class="article-meta">
              <span v-if="article.pinned" class="meta-pin">置顶</span>
              <span v-for="tag in article.tags" :key="tag" class="meta-tag"># {{ tag }}</span>
            </div>
          </div>

          <div v-if="article.coverUrl" class="article-thumb" aria-hidden="true">
            <img
              :src="article.coverUrl"
              alt=""
              class="article-thumb-img"
              loading="lazy"
              draggable="false"
            />
          </div>
        </RouterLink>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.article-index {
  min-width: 0;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border-strong);
  transition: border-color var(--duration-base) ease;
}

.heading-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.section-title {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.25;
  color: var(--color-text);
  letter-spacing: 0.04em;
}

.section-sub {
  color: var(--color-text-faint);
  font-size: 12px;
  line-height: 1.25;
  letter-spacing: 0.04em;
}

.section-meta {
  color: var(--color-text-faint);
  font-size: 12px;
}

.list-status {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-8) 0;
  color: var(--color-text-faint);
  font-size: 13.5px;
}

.retry-button {
  padding: 3px var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-text-muted);
  font-size: 12px;
  cursor: pointer;
}

.retry-button:hover {
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

.article-rows {
  display: flex;
  flex-direction: column;
}

.article-item {
  border-bottom: 1px solid var(--color-border);
  transition: border-color var(--duration-base) ease;
}

.article-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-2);
  transition:
    transform var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) ease;
}

.article-row:hover {
  transform: translateX(3px);
}

.article-row:hover .article-title {
  color: var(--color-primary);
}

.article-row:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
  border-radius: var(--radius-xs);
}

.article-num {
  display: inline-flex;
  align-items: center;
  height: 24px;
  color: var(--color-text-faint);
  font-size: 12px;
  line-height: 1;
  flex-shrink: 0;
}

.article-body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.article-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  min-height: 24px;
}

.article-title {
  font-family: var(--font-serif);
  font-size: 16.5px;
  font-weight: 600;
  line-height: 24px;
  color: var(--color-text);
  transition: color var(--duration-fast) ease;
}

.article-date {
  flex-shrink: 0;
  color: var(--color-text-faint);
  font-size: 12px;
  line-height: 1;
}

.article-summary {
  color: var(--color-text-muted);
  font-size: 13.5px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-1);
}

.meta-pin {
  padding: 1px 7px;
  border-radius: var(--radius-xs);
  background: var(--color-accent-warm-soft);
  color: var(--color-accent-warm);
  font-size: 11px;
}

.meta-tag {
  color: var(--color-text-faint);
  font-size: 11.5px;
}

.article-thumb {
  width: 84px;
  aspect-ratio: 4 / 3;
  flex-shrink: 0;
  border-radius: var(--radius-xs);
  border: 1px solid var(--color-border);
  background: var(--color-surface-muted);
  overflow: hidden;
}

.article-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

@media (max-width: 767px) {
  .article-row {
    gap: var(--space-3);
    padding: var(--space-4) 0;
  }

  .article-head {
    flex-direction: column;
    gap: 2px;
  }

  .article-thumb {
    width: 68px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .article-row:hover {
    transform: none;
  }
}
</style>
