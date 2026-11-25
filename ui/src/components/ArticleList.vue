<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useArticlesStore } from '../stores/articles'

const store = useArticlesStore()
const { articles, listStatus } = storeToRefs(store)
</script>

<template>
  <section class="article-list">
    <div class="section-heading">
      <h2 class="section-title">最新文章</h2>
      <span v-if="listStatus === 'ready'" class="section-meta">{{ articles.length }} 篇</span>
    </div>

    <p v-if="listStatus === 'loading'" class="list-status">文章加载中…</p>

    <div v-else-if="listStatus === 'error'" class="list-status">
      <span>文章加载失败</span>
      <button type="button" class="retry-button" @click="store.loadArticles(true)">重试</button>
    </div>

    <p v-else-if="listStatus === 'ready' && articles.length === 0" class="list-status">
      暂无文章。
    </p>

    <ul v-else class="article-rows">
      <li v-for="article in articles" :key="article.slug" class="article-item">
        <RouterLink
          :to="{ name: 'article-detail', params: { slug: article.slug } }"
          class="article-row"
          :aria-label="`阅读：${article.title}`"
        >
          <div class="article-thumb" aria-hidden="true">
            <img
              v-if="article.coverUrl"
              :src="article.coverUrl"
              alt=""
              class="article-thumb-img"
              loading="lazy"
              draggable="false"
            />
          </div>
          <div class="article-body">
            <div class="article-title">{{ article.title }}</div>
            <p class="article-summary">{{ article.summary }}</p>
            <div class="article-meta">
              <span>{{ article.publishedAt.slice(0, 10) }}</span>
              <span v-if="article.tags.length" class="article-tags">{{ article.tags.join(' · ') }}</span>
            </div>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.article-list {
  min-width: 0;
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.section-title {
  font-size: 17px;
  font-weight: 600;
}

.section-meta {
  color: var(--color-text-faint);
  font-size: 12px;
}

.list-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding-block: var(--space-4);
  color: var(--color-text-muted);
  font-size: 13px;
}

.retry-button {
  padding: 2px var(--space-3);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-nav-surface);
  color: var(--color-text);
  font-size: 12px;
  cursor: pointer;
}

.article-rows {
  display: flex;
  flex-direction: column;
  padding: 0;
  margin: 0;
  list-style: none;
}

.article-item {
  border-bottom: 1px solid var(--color-border);
}

.article-item:last-child {
  border-bottom: 0;
}

.article-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-block: var(--space-2);
  border-radius: var(--radius-sm);
}

.article-item:last-child .article-row {
  padding-bottom: 0;
}

.article-row:hover .article-title {
  color: var(--color-primary);
}

.article-row:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
}

.article-thumb {
  width: 80px;
  aspect-ratio: 4 / 3;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--color-nav-surface);
  overflow: hidden;
}

.article-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.article-body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.article-title {
  font-size: 15px;
  font-weight: 600;
}

.article-summary {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  color: var(--color-text-faint);
  font-size: 11px;
}

.article-tags {
  color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .article-list {
    padding: var(--space-4);
  }

  .article-thumb {
    width: 68px;
  }
}
</style>
