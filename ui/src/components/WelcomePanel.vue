<script setup lang="ts">
</script>

<template>
  <section class="welcome-panel">
    <div class="welcome-intro">
      <div class="welcome-copy">
        <h2 class="welcome-title">～(∠・ω< )⌒★</h2>
        <p class="welcome-text">......</p>
      </div>
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

.welcome-title {
  font-size: 17px;
  font-weight: 600;
}

.welcome-text {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 13px;
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
