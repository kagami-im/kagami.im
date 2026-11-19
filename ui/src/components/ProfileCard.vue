<template>
  <section class="profile-card">
    <div class="profile-head">
      <span class="profile-avatar" aria-hidden="true">K</span>
      <div class="profile-identity">
        <h2 class="profile-name">Kagami</h2>
        <p class="profile-bio">这是介绍。</p>
      </div>
    </div>

    <div class="profile-links">
      <span class="profile-link">GitHub</span>
      <span class="profile-link">Email</span>
      <span class="profile-link">RSS</span>
    </div>
  </section>
</template>

<style scoped>
.profile-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.profile-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.profile-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-avatar);
  color: var(--color-text);
  font-size: 18px;
  font-weight: 700;
}

.profile-identity {
  min-width: 0;
}

.profile-name {
  font-size: 17px;
  font-weight: 600;
}

.profile-bio {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.profile-links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.profile-link {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--color-nav-surface);
  color: var(--color-text-muted);
  font-size: 11px;
}
</style>
