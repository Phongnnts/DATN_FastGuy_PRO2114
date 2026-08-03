<script setup>
import { computed, ref } from 'vue';
import PublicContentLayout from '@/components/common/PublicContentLayout.vue';

const query = ref('');
const sections = [
  { title: 'Đặt món và tài khoản', items: [
    { question: 'Tôi có cần tài khoản để đặt món không?', answer: 'Không. Bạn có thể đặt món với số điện thoại và địa chỉ nhận hàng. Đăng nhập giúp lưu thông tin, xem lịch sử đơn và gửi yêu cầu hỗ trợ thuận tiện hơn.' },
    { question: 'Làm sao thay đổi thông tin tài khoản?', answer: 'Sau khi đăng nhập, mở trang Hồ sơ để cập nhật họ tên, số điện thoại và địa chỉ. Một số thay đổi có thể cần xác minh để bảo vệ tài khoản.' },
  ]},
  { title: 'Giao hàng', items: [
    { question: 'FastGuy giao hàng trong bao lâu?', answer: 'Thời gian dự kiến hiển thị khi xác nhận đơn và có thể thay đổi theo khoảng cách, thời tiết hoặc lượng đơn. Bạn có thể theo dõi trạng thái bằng mã đơn hàng.' },
    { question: 'Tôi chưa nhận được món nhưng đơn báo đã giao?', answer: 'Hãy kiểm tra với người nhận, bảo vệ hoặc khu vực giao hàng. Nếu vẫn chưa thấy đơn, đăng nhập và gửi yêu cầu hỗ trợ kèm mã đơn để FastGuy kiểm tra.' },
  ]},
  { title: 'Thanh toán', items: [
    { question: 'Tôi có thể thanh toán bằng cách nào?', answer: 'Phương thức khả dụng được hiển thị tại bước thanh toán, gồm thanh toán khi nhận hàng hoặc cổng thanh toán trực tuyến tùy từng thời điểm.' },
    { question: 'Thanh toán trực tuyến thất bại nhưng tài khoản đã bị trừ tiền?', answer: 'Không thanh toán lại ngay. Hãy kiểm tra trạng thái đơn và lịch sử giao dịch. Khoản giữ tiền thường được ngân hàng hoàn tự động; nếu đơn không được xác nhận, liên hệ hỗ trợ kèm mã giao dịch.' },
  ]},
  { title: 'Thay đổi và hủy đơn', items: [
    { question: 'Tôi có thể sửa hoặc hủy đơn không?', answer: 'Bạn chỉ có thể yêu cầu thay đổi hoặc hủy trước khi cửa hàng bắt đầu chuẩn bị món. Hãy liên hệ hỗ trợ sớm nhất; FastGuy không bảo đảm hủy được khi đơn đã chế biến hoặc đang giao.' },
    { question: 'Khi nào tôi được hoàn tiền?', answer: 'Nếu đơn đủ điều kiện hoàn tiền, FastGuy xác nhận sau khi kiểm tra. Tiền được trả về phương thức ban đầu; thời gian ghi có phụ thuộc ngân hàng hoặc đơn vị thanh toán.' },
  ]},
];
const normalizedQuery = computed(() => query.value.trim().toLocaleLowerCase('vi'));
const filteredSections = computed(() => sections.map(section => ({ ...section, items: section.items.filter(item => `${item.question} ${item.answer}`.toLocaleLowerCase('vi').includes(normalizedQuery.value)) })).filter(section => section.items.length));
</script>

<template>
  <PublicContentLayout eyebrow="Trung tâm trợ giúp" title="FastGuy có thể giúp gì cho bạn?" intro="Tìm câu trả lời về đặt món, giao hàng, thanh toán và xử lý đơn hàng.">
    <section class="help-search" aria-labelledby="faq-heading">
      <h2 id="faq-heading">Câu hỏi thường gặp</h2>
      <label for="faq-search">Tìm trong câu hỏi thường gặp</label>
      <input id="faq-search" v-model="query" type="search" placeholder="Ví dụ: hủy đơn, hoàn tiền" autocomplete="off" />
      <p class="result-count" role="status" aria-live="polite">{{ filteredSections.reduce((total, section) => total + section.items.length, 0) }} kết quả</p>
    </section>

    <div v-if="filteredSections.length" class="faq-groups">
      <section v-for="section in filteredSections" :key="section.title" class="faq-group" :aria-labelledby="`faq-${section.title}`">
        <h2 :id="`faq-${section.title}`">{{ section.title }}</h2>
        <details v-for="item in section.items" :key="item.question">
          <summary>{{ item.question }}</summary>
          <p>{{ item.answer }}</p>
        </details>
      </section>
    </div>
    <p v-else class="empty" role="status">Không tìm thấy câu trả lời phù hợp. Hãy thử từ khóa khác hoặc liên hệ hỗ trợ.</p>

    <section class="guidance" aria-label="Hướng dẫn nhanh">
      <article><h2>Giao hàng</h2><p>Giữ điện thoại hoạt động, kiểm tra địa chỉ và ghi chú điểm nhận rõ ràng. Dùng mã đơn cùng 4 số cuối điện thoại để xem tiến trình.</p></article>
      <article><h2>Thanh toán</h2><p>Kiểm tra tổng tiền, ưu đãi và phương thức trước khi xác nhận. Không cung cấp mã OTP hoặc mật khẩu cho người giao hàng.</p></article>
      <article><h2>Hủy đơn</h2><p>Gửi yêu cầu ngay khi cần. Đơn đã chuẩn bị có thể không hủy được và phí phát sinh sẽ được thông báo trước khi xử lý.</p></article>
    </section>

    <section class="support-cta" aria-labelledby="support-heading">
      <div><h2 id="support-heading">Vẫn cần trợ giúp?</h2><p>Tra cứu đơn ngay hoặc đăng nhập để gửi và theo dõi yêu cầu hỗ trợ.</p></div>
      <div class="actions"><RouterLink to="/track-order">Theo dõi đơn</RouterLink><RouterLink class="secondary" to="/login">Đăng nhập tài khoản</RouterLink></div>
    </section>
  </PublicContentLayout>
</template>

<style scoped>
.help-search{margin-bottom:28px}.help-search h2{margin:0 0 16px;font-size:24px}.help-search label{display:block;margin-bottom:8px;font-size:14px;font-weight:700}.help-search input{width:100%;min-height:50px;padding:0 16px;border:1px solid var(--border,#d8d1cb);border-radius:10px;background:#fff;color:inherit;font:inherit}.help-search input:focus-visible{outline:3px solid rgba(212,118,74,.28);border-color:var(--primary,#d4764a)}.result-count{margin:8px 0 0;color:var(--text-mid,#625b55);font-size:13px}.faq-groups{display:grid;gap:22px}.faq-group{padding:26px;border:1px solid var(--border,#e5ded8);border-radius:18px;background:#fff}.faq-group h2{margin:0 0 8px;font-size:21px}.faq-group details{border-bottom:1px solid var(--border-light,#eee8e3)}.faq-group details:last-child{border-bottom:0}.faq-group summary{padding:17px 34px 17px 0;font-weight:700;line-height:1.45;cursor:pointer}.faq-group summary:focus-visible{outline:3px solid rgba(212,118,74,.28);outline-offset:3px}.faq-group p{margin:0;padding:0 0 18px;color:var(--text-mid,#625b55);line-height:1.7}.empty{padding:30px;text-align:center;border:1px solid var(--border,#e5ded8);border-radius:16px;background:#fff}.guidance{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin:36px 0}.guidance article{padding:22px;border-radius:15px;background:#1b1714;color:#fff}.guidance h2{margin:0 0 10px;font-size:18px}.guidance p{margin:0;color:rgba(255,255,255,.7);font-size:14px;line-height:1.65}.support-cta{display:flex;align-items:center;justify-content:space-between;gap:28px;padding:28px;border-radius:18px;background:var(--primary,#d4764a);color:#fff}.support-cta h2{margin:0 0 7px}.support-cta p{margin:0;color:rgba(255,255,255,.82)}.actions{display:flex;flex-wrap:wrap;gap:10px}.actions a{display:inline-flex;align-items:center;justify-content:center;min-height:44px;padding:0 17px;border:1px solid #fff;border-radius:999px;background:#fff;color:#7a351c;text-decoration:none}.actions .secondary{background:transparent;color:#fff}@media(max-width:720px){.guidance{grid-template-columns:1fr}.support-cta{align-items:stretch;flex-direction:column}.actions a{flex:1}}@media(max-width:440px){.actions{flex-direction:column}}
</style>
