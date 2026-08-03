<script setup>
defineProps({
  eyebrow: { type: String, required: true },
  title: { type: String, required: true },
  intro: { type: String, required: true },
  updated: { type: String, default: '' },
});
</script>

<template>
  <div class="public-page">
    <header class="public-hero">
      <div class="container public-hero__inner">
        <p class="eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <p class="intro">{{ intro }}</p>
        <p v-if="updated" class="updated">Cập nhật lần cuối: {{ updated }}</p>
      </div>
    </header>
    <div class="container public-content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.public-page{min-height:100vh;background:var(--bg,#f7f4f0);color:var(--text-dark,#211b17)}.public-hero{padding:64px 0 54px;color:#fff;background:#1b1714}.public-hero__inner{max-width:900px}.eyebrow{margin:0 0 12px;color:var(--route-amber,#f5b942);font-size:12px;font-weight:800;letter-spacing:.13em;text-transform:uppercase}.public-hero h1{max-width:760px;margin:0 0 16px;font-size:clamp(34px,6vw,56px);line-height:1.08;letter-spacing:-.04em}.intro{max-width:720px;margin:0;color:rgba(255,255,255,.76);font-size:17px;line-height:1.7}.updated{margin:18px 0 0;color:rgba(255,255,255,.58);font-size:13px}.public-content{max-width:900px;padding-top:48px;padding-bottom:72px}:deep(.content-section){margin-bottom:36px;padding:28px;border:1px solid var(--border,#e5ded8);border-radius:18px;background:#fff;box-shadow:var(--shadow-sm,0 4px 18px rgba(30,20,14,.05))}:deep(.content-section h2){margin:0 0 14px;font-size:23px;line-height:1.3}:deep(.content-section h3){margin:22px 0 8px;font-size:17px}:deep(.content-section p),:deep(.content-section li){color:var(--text-mid,#625b55);line-height:1.75}:deep(.content-section p){margin:0 0 12px}:deep(.content-section p:last-child){margin-bottom:0}:deep(.content-section ul),:deep(.content-section ol){margin:10px 0 0;padding-left:22px}:deep(a){color:var(--primary,#d4764a);font-weight:700;text-underline-offset:3px}:deep(a:focus-visible){outline:3px solid rgba(212,118,74,.3);outline-offset:3px;border-radius:3px}@media(max-width:640px){.public-hero{padding:44px 0 40px}.public-content{padding-top:28px;padding-bottom:48px}:deep(.content-section){margin-bottom:20px;padding:21px;border-radius:14px}.intro{font-size:15px}}
</style>
