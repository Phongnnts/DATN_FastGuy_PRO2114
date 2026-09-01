<template>
  <header class="admin-page-header">
    <div class="admin-page-header__copy">
      <h1 class="admin-page-header__title"><slot name="title" /></h1>
      <div class="admin-page-header__description"><slot name="description" /></div>
    </div>
    <div class="admin-page-header__actions" aria-label="Tác vụ trang"><slot name="actions" /></div>
  </header>
</template>

<style scoped>
.admin-page-header{display:flex;align-items:flex-start;justify-content:space-between;gap:var(--space-6);color:var(--admin-foreground)}
.admin-page-header__copy{display:grid;min-width:0;gap:var(--space-2)}
.admin-page-header__title{margin:0;font-size:clamp(1.5rem,3vw,2rem);font-weight:750;line-height:1.2;letter-spacing:-.025em}
.admin-page-header__description{max-width:72ch;color:var(--admin-muted);line-height:1.6}
.admin-page-header__actions{display:flex;flex:none;align-items:center;justify-content:flex-end;gap:var(--space-2);flex-wrap:wrap}
@media(max-width:640px){.admin-page-header{align-items:stretch;flex-direction:column;gap:var(--space-4)}.admin-page-header__actions{justify-content:flex-start}}
</style>
