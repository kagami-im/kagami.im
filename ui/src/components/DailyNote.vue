<template>
  <section class="daily-note">
    <h2 class="note-title">......</h2>
    <p class="note-text">......</p>
  </section>
</template>

<style scoped>
.daily-note {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  background: var(--color-welcome-surface);
  box-shadow: var(--shadow-soft);
}

.note-title {
  font-size: 15px;
  font-weight: 600;
}

.note-text {
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.5;
}
</style>
