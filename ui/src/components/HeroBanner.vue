<script setup lang="ts">
import { computed } from 'vue'
import { usePlayerStore } from '../stores/player'

const playerStore = usePlayerStore()

interface WaveOptions {
  amplitude: number
  baseline: number
  bottom: number
  end: number
  inverted?: boolean
  period: number
  start: number
}

function buildWavePath({
  amplitude,
  baseline,
  bottom,
  end,
  inverted = false,
  period,
  start,
}: WaveOptions): string {
  const direction = inverted ? -1 : 1
  let path = `M ${start} ${baseline}`
  for (let x = start; x < end; x += period) {
    path += ` C ${x + period / 8} ${baseline - amplitude * direction}`
    path += ` ${x + (period * 3) / 8} ${baseline - amplitude * direction}`
    path += ` ${x + period / 2} ${baseline}`
    path += ` C ${x + (period * 5) / 8} ${baseline + amplitude * direction}`
    path += ` ${x + (period * 7) / 8} ${baseline + amplitude * direction}`
    path += ` ${x + period} ${baseline}`
  }
  return `${path} V ${bottom} H ${start} Z`
}

const shared = { bottom: 60, end: 1200, period: 120, start: -120 }
const waveBack = computed(() =>
  buildWavePath({ ...shared, amplitude: 12, baseline: 22, inverted: true })
)
const waveFront = computed(() => buildWavePath({ ...shared, amplitude: 10, baseline: 28 }))
</script>

<template>
  <section class="hero-banner" aria-label="首页横幅" @dblclick="playerStore.expandPlayer()">
    <svg
      class="banner-waves"
      viewBox="0 0 1200 60"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <path :d="waveBack" fill="var(--color-bg)" opacity="0.7" />
      <path :d="waveFront" fill="var(--color-bg)" />
    </svg>
  </section>
</template>

<style scoped>
/* Scale the artwork to cover the full viewport width and two-thirds of its
   height, cropping the excess rather than leaving gaps or stretching it. */
.hero-banner {
  position: relative;
  isolation: isolate;
  width: 100vw;
  height: 66.667vh;
  margin-inline: calc(50% - 50vw);
  overflow: hidden;
  isolation: isolate;
  background: var(--color-hero-surface) url('/heroBanner.webp') center / cover no-repeat;
}

.banner-waves {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  display: block;
  width: 100%;
  height: 60px;
}

/* Dark mode dims the bright pastel artwork so it doesn't glare against the
   dark page. The dimmer is a backdrop-filter on ::before: it darkens only the
   artwork painted on the container background, while the SVG waves above it
   keep their exact --color-bg match with the page below. */
html.dark .hero-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  backdrop-filter: brightness(0.75) saturate(0.85);
}
</style>
