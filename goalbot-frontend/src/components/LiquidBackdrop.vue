<template>
  <div class="liquid-backdrop" aria-hidden="true">
    <span class="liquid-ribbon liquid-ribbon-brand" />
    <span class="liquid-ribbon liquid-ribbon-accent" />
    <span class="liquid-ribbon liquid-ribbon-mist" />
  </div>
</template>

<style scoped>
.liquid-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--bg);
}

.liquid-ribbon {
  position: absolute;
  display: block;
  width: 145vw;
  min-width: 980px;
  border-radius: 48% 52% 44% 56% / 58% 42% 58% 42%;
  filter: blur(58px) saturate(1.18);
  transform-origin: center;
  will-change: transform;
}

.liquid-ribbon-brand {
  top: -38vh;
  left: -24vw;
  height: 66vh;
  min-height: 430px;
  background: var(--liquid-brand);
  opacity: .34;
  animation: liquid-current-brand 18s ease-in-out infinite alternate;
}

.liquid-ribbon-accent {
  top: 36vh;
  left: -52vw;
  width: 138vw;
  height: 34vh;
  min-height: 250px;
  background: var(--liquid-accent);
  border-radius: 40% 60% 58% 42% / 46% 54% 38% 62%;
  opacity: .22;
  animation: liquid-current-accent 23s ease-in-out infinite alternate;
}

.liquid-ribbon-mist {
  right: -42vw;
  bottom: -40vh;
  width: 132vw;
  height: 70vh;
  min-height: 460px;
  background: var(--liquid-mist);
  border-radius: 58% 42% 46% 54% / 62% 38% 56% 44%;
  opacity: .58;
  animation: liquid-current-mist 27s ease-in-out infinite alternate;
}

@keyframes liquid-current-brand {
  from { transform: translate3d(-3vw, -1vh, 0) rotate(-7deg) scaleY(.94); }
  to { transform: translate3d(5vw, 5vh, 0) rotate(-3deg) scaleY(1.08); }
}

@keyframes liquid-current-accent {
  from { transform: translate3d(-4vw, 2vh, 0) rotate(8deg) scaleX(.94); }
  to { transform: translate3d(8vw, -4vh, 0) rotate(3deg) scaleX(1.08); }
}

@keyframes liquid-current-mist {
  from { transform: translate3d(4vw, 3vh, 0) rotate(-5deg) scale(1); }
  to { transform: translate3d(-6vw, -3vh, 0) rotate(2deg) scale(1.08); }
}

@media (max-width: 640px) {
  .liquid-ribbon { min-width: 720px; filter: blur(48px) saturate(1.14); }
  .liquid-ribbon-brand { left: -88vw; opacity: .31; }
  .liquid-ribbon-accent { top: 42vh; left: -112vw; opacity: .2; }
  .liquid-ribbon-mist { right: -104vw; opacity: .55; }
}

@media (prefers-reduced-motion: reduce) {
  .liquid-ribbon { animation: none; }
}
</style>
