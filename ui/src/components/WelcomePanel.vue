<script setup lang="ts">
const quickEntries = ['写作', '开发', '番剧', '摄影']
</script>

<template>
  <section class="welcome-panel">
    <div class="welcome-intro">
      <span class="welcome-avatar" aria-hidden="true">K</span>
      <div class="welcome-copy">
        <h2 class="welcome-title">欢迎！</h2>
        <p class="welcome-text">这是描述。</p>
      </div>
    </div>

    <div class="quick-entries">
      <button v-for="entry in quickEntries" :key="entry" type="button" class="quick-entry">
        {{ entry }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.welcome-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  background: var(--color-welcome-surface);
  box-shadow: var(--shadow-soft);
}

.welcome-intro {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.welcome-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-avatar);
  color: var(--color-text);
  font-size: 17px;
  font-weight: 700;
}

.welcome-title {
  font-size: 17px;
  font-weight: 600;
}

.welcome-text {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 13px;
}

.quick-entries {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.quick-entry {
  min-width: 60px;
  padding: var(--space-2) var(--space-3);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.quick-entry:hover {
  color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .welcome-panel {
    padding: var(--space-3) var(--space-4);
  }

  .welcome-title {
    font-size: 16px;
  }
}
</style>
