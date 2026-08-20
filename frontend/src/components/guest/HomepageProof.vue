<script setup>
import { ref } from 'vue';
import { isTrustedHomepageAvatar } from '@/utils/homepage';

const props = defineProps({ featuredReviews: { type: Array, default: () => [] }, reviewError: { type: String, default: '' } });
const invalidAvatars = ref(new Set());
const reasons = [
  { icon: 'bi-receipt', title: 'Món rõ giá, dễ chọn', copy: 'Thông tin món và giá luôn hiển thị rõ ràng.' },
  { icon: 'bi-sliders', title: 'Tùy chỉnh theo khẩu vị', copy: 'Chọn phiên bản và tùy chọn phù hợp với bạn.' },
  { icon: 'bi-geo-alt', title: 'Theo dõi đơn minh bạch', copy: 'Biết đơn hàng đang ở đâu trong từng bước.' },
  { icon: 'bi-chat-heart', title: 'Hỗ trợ khi cần', copy: 'Có kênh hỗ trợ rõ ràng khi bạn cần trợ giúp.' },
];

function initials(name) { return (name || 'FG').trim().split(/\s+/).slice(0, 2).map(word => word[0]).join('').toUpperCase(); }
function hasAvatar(review) { return isTrustedHomepageAvatar(review.avatarUrl) && !invalidAvatars.value.has(review.reviewId); }
function verifyAvatar(event, review) { if (event.target.naturalWidth > 0) return; invalidAvatars.value = new Set([...invalidAvatars.value, review.reviewId]); }
function hideAvatar(review) { invalidAvatars.value = new Set([...invalidAvatars.value, review.reviewId]); }
</script>

<template>
  <section class="proof" aria-labelledby="proof-title">
    <div class="container">
      <div class="section-head"><div><p>Vì sao chọn FastGuy</p><h2 id="proof-title">Một bữa ngon nên bắt đầu<br>từ trải nghiệm dễ dàng</h2></div><span>Rõ ràng khi chọn món. Chủ động trong từng bước. Nhẹ nhàng từ lúc đặt đến khi nhận.</span></div>
      <ul class="reasons" aria-label="Lý do chọn FastGuy"><li v-for="(reason, index) in reasons" :key="reason.title"><div class="reason-icon"><i :class="`bi ${reason.icon}`" aria-hidden="true"></i></div><div class="reason-copy"><small>0{{ index + 1 }}</small><strong>{{ reason.title }}</strong><span>{{ reason.copy }}</span></div></li></ul>
      <div class="reviews-head"><div><p>Khách ăn gì nói gì?</p><h3>Trải nghiệm thật từ khách hàng FastGuy</h3></div><span v-if="props.featuredReviews.length"><i class="bi bi-star-fill" aria-hidden="true"></i> Đánh giá nổi bật</span></div>
      <div class="proof-layout" :class="{ 'reasons-full': !props.featuredReviews.length }">
        <div v-if="props.featuredReviews.length" class="reviews" aria-label="Đánh giá nổi bật">
          <blockquote v-for="review in props.featuredReviews" :key="review.reviewId">
            <i class="bi bi-quote" aria-hidden="true"></i><p v-if="review.comment">“{{ review.comment }}”</p>
            <footer><img v-if="hasAvatar(review)" :src="review.avatarUrl" :alt="`Ảnh đại diện ${review.userName}`" @load="verifyAvatar($event, review)" @error="hideAvatar(review)"><span v-else class="avatar-fallback" aria-hidden="true">{{ initials(review.userName) }}</span><span><strong>{{ review.userName }}</strong><small :aria-label="`${review.rating} sao trên 5`">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }} · {{ review.rating }}/5</small></span></footer>
          </blockquote>
        </div>
        <p v-else-if="props.reviewError" class="review-state" role="status" aria-live="polite">Không thể tải đánh giá. Vui lòng thử lại sau.</p>
        <p v-else class="review-state" role="status">Chưa có đánh giá nổi bật.</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.proof{padding:108px 0 60px;background:#fff}.section-head{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(260px,.65fr);align-items:end;gap:50px;margin-bottom:46px}.section-head p,.reviews-head p{margin:0 0 10px;color:#f26a2e;font-size:10px;font-weight:900;letter-spacing:.16em;text-transform:uppercase}.section-head h2{font-size:clamp(42px,5.5vw,68px);font-weight:900;line-height:.98;letter-spacing:-.06em}.section-head>span{max-width:390px;padding-bottom:6px;color:#806e63;font-size:14px;line-height:1.7}.reasons{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;margin:0;padding:0;list-style:none}.reasons li{position:relative;display:grid;grid-template-columns:82px minmax(0,1fr);align-items:center;gap:24px;min-height:190px;padding:30px 32px;border:1px solid #eee3dc;border-radius:24px;background:linear-gradient(135deg,#fffaf6 0%,#fff 72%);box-shadow:0 12px 34px rgba(43,25,15,.045);transition:border-color .22s ease,box-shadow .22s ease,transform .22s ease}.reasons li:hover{border-color:rgba(242,106,46,.42);box-shadow:0 20px 44px rgba(43,25,15,.09);transform:translateY(-3px)}.reason-icon{display:grid;width:64px;height:64px;place-items:center;border-radius:19px;color:#fff;background:#f26a2e;box-shadow:0 12px 24px rgba(242,106,46,.2);font-size:27px;transform:rotate(-3deg)}.reason-copy small,.reasons strong,.reasons span{display:block}.reason-copy small{margin-bottom:8px;color:#c8b8ae;font-size:9px;font-weight:900;letter-spacing:.14em}.reasons strong{margin-bottom:10px;font-size:clamp(18px,2vw,24px);font-weight:850;line-height:1.15;letter-spacing:-.035em}.reasons span{max-width:420px;color:#806e63;font-size:13px;line-height:1.65}.reviews-head{display:flex;align-items:end;justify-content:space-between;gap:24px;margin:78px 0 24px}.reviews-head h3{font-size:clamp(25px,3vw,36px);letter-spacing:-.04em}.reviews-head>span{display:flex;align-items:center;gap:6px;color:#725f54;font-size:11px;font-weight:750}.reviews-head>span i{color:#f26a2e}.proof-layout.reasons-full{display:block}.reviews{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.reviews blockquote{position:relative;min-width:0;min-height:225px;margin:0;padding:25px;border-radius:22px;color:#fff;background:#1b1714}.reviews blockquote>i{position:absolute;top:17px;right:20px;color:rgba(255,200,87,.35);font-size:34px}.reviews blockquote>p{display:-webkit-box;overflow:hidden;min-height:84px;margin:14px 0 24px;font-size:14px;line-height:1.65;-webkit-box-orient:vertical;-webkit-line-clamp:4}.reviews footer{display:flex;align-items:center;gap:10px}.reviews img,.avatar-fallback{width:40px;height:40px;flex:0 0 40px;border-radius:50%;object-fit:cover}.avatar-fallback{display:grid;place-items:center;color:#1b1714;background:#ffc857;font-size:11px;font-weight:900}.reviews strong,.reviews small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.reviews strong{font-size:12px}.reviews small{margin-top:4px;color:#ffc857;font-size:10px}.review-state{padding:30px;border:1px dashed #dfd1c7;border-radius:18px;color:#7d6b60;text-align:center;background:#fffaf6}@media(max-width:900px){.section-head{grid-template-columns:1fr;gap:20px}.section-head>span{max-width:620px}.reviews{grid-template-columns:repeat(3,minmax(0,1fr))}.reasons li{grid-template-columns:70px 1fr;padding:25px 24px}}@media(max-width:620px){.proof{padding:72px 0 40px}.section-head{margin-bottom:30px}.section-head h2{font-size:clamp(38px,12vw,52px)}.section-head h2 br{display:none}.section-head>span{font-size:13px}.reasons{grid-template-columns:1fr;gap:10px}.reasons li{grid-template-columns:64px 1fr;gap:16px;min-height:142px;padding:20px;border-radius:18px}.reason-icon{width:56px;height:56px;border-radius:17px;font-size:23px}.reasons strong{font-size:20px}.reasons span{font-size:12px}.reviews-head{align-items:flex-start;flex-direction:column;margin-top:56px}.reviews{grid-template-columns:1fr}.reviews blockquote{min-height:190px}.reviews blockquote>p{min-height:auto}}@media(prefers-reduced-motion:reduce){.reasons li{transition:none}.reasons li:hover{transform:none}}
</style>
