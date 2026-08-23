import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');

test('homepage integrates commerce sections from existing homepage contract', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.doesNotMatch(source, /HomepageOccasions|occasionCombos/);
  assert.match(source, /Hôm nay ăn gì\?/);
  assert.match(source, /role="tablist"/);
  assert.match(source, /Đang được yêu thích/);
  assert.match(source, /class="ranking-list"/);
  assert.match(source, /Mới lên bếp/);
  assert.match(source, /v-if="newProducts\.length"/);
});

test('signature homepage establishes one product-led focal point and commerce-first order', async () => {
  const page = await read('../src/views/guest/HomePage.vue');
  assert.match(page, /signatureHomepageProduct/);
  assert.match(page, /class="signature-product"/);
  assert.match(page, /class="promo-strip"/);
  assert.ok(page.indexOf('<FeaturedProducts') < page.indexOf('id="recommendation-title"'));
  assert.ok(page.indexOf('id="trending-title"') < page.indexOf('class="brand-manifesto"'));
  assert.doesNotMatch(page, /class="experience-section"|class="home-reasons"|class="home-cta"/);
});

test('homepage closes with four meaningful brand chapters then configured Google Map', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.match(source, /class="brand-manifesto"/);
  assert.match(source, /Bận không có nghĩa[\s\S]*là ăn qua loa/);
  assert.match(source, /Cuộc sống có thể vội\.[\s\S]*Bữa ăn thì không/);
  assert.match(source, /class="making-story"/);
  assert.match(source, /Nhanh ở việc đặt\.[\s\S]*Không vội ở việc làm món/);
  assert.match(source, /Nguyên liệu mỗi ngày[\s\S]*Làm khi có đơn[\s\S]*Kiểm tra và đóng gói/);
  assert.match(source, /class="people-time"/);
  assert.match(source, /Có người đứng sau[\s\S]*mỗi món ăn/);
  assert.match(source, /Nhanh để bạn[\s\S]*không phải sống vội/);
  assert.match(source, /class="account-prompt"/);
  assert.match(source, /Hôm nay bạn muốn ăn gì\?/);
  const chapters = ['class="brand-manifesto"', 'class="making-story"', 'class="people-time"', 'class="account-prompt"', 'class="store-location"'];
  chapters.slice(1).forEach((chapter, index) => assert.ok(source.indexOf(chapters[index]) < source.indexOf(chapter)));
  assert.match(source, /v-if="storeAddress" class="store-location"/);
  assert.match(source, /:src="mapEmbedUrl"/);
  assert.match(source, /:href="mapOpenUrl"/);
});

test('storytelling uses balanced container grids, content-driven height and one lifestyle image', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.doesNotMatch(source, /Bận không có nghĩa<br>|Có người đứng sau<br>|Nhanh để bạn<br>|Nhanh ở việc đặt\.<br>/);
  assert.doesNotMatch(source, /Cuộc sống có thể vội\.<br>/);
  assert.match(source, /\.manifesto-shell\{[^}]*grid-template-columns:minmax\(0,\.9fr\) minmax\(0,1\.1fr\)/);
  assert.match(source, /\.manifesto-copy,.people-copy\{[^}]*min-width:0/);
  assert.match(source, /\.manifesto-copy h2[^}]*font-size:clamp\(/);
  assert.match(source, /\.people-time[^}]*padding:/);
  assert.doesNotMatch(source, /\.manifesto-shell\{[^}]*min-height|\.people-shell\{[^}]*min-height/);
  assert.doesNotMatch(source, /\.manifesto-shell\{[^}]*padding-inline:0|\.people-shell\{[^}]*padding-inline:0/);
  assert.doesNotMatch(source, /class="people-visual"/);
  assert.equal((source.match(/fastguy-home-story_lfyhtg\.jpg/g) || []).length, 1);
  assert.match(source, /@media\(max-width:900px\)[\s\S]*\.manifesto-shell[^}]*grid-template-columns:1fr/);
  assert.match(source, /@media\(prefers-reduced-motion:reduce\)/);
});

test('homepage adds truthful account, review, brand promise and footer sections', async () => {
  const source = await read('../src/views/guest/HomePage.vue');
  assert.match(source, /useAuthStore/);
  assert.match(source, /auth\.isUser/);
  assert.match(source, /\/account\/orders/);
  assert.match(source, /homepageStore\.featuredReviews/);
  assert.match(source, /Khách hàng nói gì/);
  assert.match(source, /Đánh giá trước/);
  assert.match(source, /Bận không có nghĩa[\s\S]*là ăn qua loa/);
  assert.match(source, /Nhanh ở việc đặt\.[\s\S]*Không vội ở việc làm món/);
  assert.doesNotMatch(source, /class="home-footer"/);
  assert.match(source, /\.ranking-list a:hover,[\s\S]*\.ranking-list a:focus-visible/);
  assert.match(source, /background:#f26a2e/);
  assert.doesNotMatch(source, /1\.240 điểm|Đặt lại món|Đã đặt \d+ lần/);
});
