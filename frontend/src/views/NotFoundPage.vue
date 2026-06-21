<template>
  <div class="not-found-page">
    <div class="container" style="text-align: center; padding: 80px 20px">
      <i
        class="bi bi-emoji-frown"
        style="
          font-size: 80px;
          color: var(--text-light);
          display: block;
          margin-bottom: 20px;
        "
      ></i>
      <h1 style="font-size: 48px; font-weight: 800; margin-bottom: 8px">404</h1>
      <p style="font-size: 18px; color: var(--text-mid); margin-bottom: 32px">
        Trang bạn tìm không tồn tại
      </p>
      <router-link to="/" class="btn btn-lg btn-primary"
        ><i class="bi bi-house"></i> Về trang chủ</router-link
      >
    </div>
  </div>
</template>
