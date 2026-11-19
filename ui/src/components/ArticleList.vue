<template>
  <section class="article-list">
    <div class="section-heading">
      <h2 class="section-title">最新文章</h2>
      <span class="section-meta">3 篇</span>
    </div>

    <ul class="article-rows">
      <li v-for="row in 3" :key="row" class="article-row">
        <div class="article-thumb" aria-hidden="true"></div>
        <div class="article-body">
          <div class="skeleton skeleton--title" aria-hidden="true"></div>
          <div class="skeleton skeleton--text" aria-hidden="true"></div>
          <div class="skeleton skeleton--meta" aria-hidden="true"></div>
        </div>
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

.article-rows {
  display: flex;
  flex-direction: column;
  padding: 0;
  margin: 0;
  list-style: none;
}

.article-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-block: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.article-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.article-thumb {
  width: 80px;
  aspect-ratio: 4 / 3;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--color-nav-surface);
}

.article-body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.skeleton {
  height: 8px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-muted);
}

.skeleton--title {
  width: 72%;
  height: 12px;
}

.skeleton--text {
  width: 94%;
}

.skeleton--meta {
  width: 42%;
  height: 6px;
}

.article-row:nth-child(2) .skeleton--title {
  width: 84%;
}

.article-row:nth-child(2) .skeleton--meta {
  width: 36%;
}

.article-row:nth-child(3) .skeleton--title {
  width: 66%;
}

.article-row:nth-child(3) .skeleton--meta {
  width: 48%;
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
