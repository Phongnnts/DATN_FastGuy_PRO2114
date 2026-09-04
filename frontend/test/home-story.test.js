import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const homeUrl = new URL('../src/views/guest/HomePage.vue', import.meta.url);

test('home tells the approved signature story with a product-led CTA', async () => {
  const source = await readFile(homeUrl, 'utf8');
  assert.match(source, /Bữa ngon cho ngày bận rộn/);
  assert.match(source, /class="signature-product"/);
  assert.match(source, /signatureProduct\.productId/);
  assert.match(source, /Bận không có nghĩa[\s\S]*là ăn qua loa/);
  assert.match(source, /Nhanh ở việc đặt\.[\s\S]*Không vội ở việc làm món/);
  assert.match(source, /Nhanh để bạn[\s\S]*không phải sống vội/);
  assert.match(source, /fastguy-delivery-story\.jpg/);
  assert.match(source, /alt="Nhân viên FastGuy giao bữa ăn cho khách hàng"/);
});

test('home removes duplicate ordering guide and unsupported metrics', async () => {
  const source = await readFile(homeUrl, 'utf8');
  assert.doesNotMatch(source, /class="experience-section"|class="home-reasons"|class="home-cta"/);
  assert.doesNotMatch(source, /10,000\+|4\.8<small>\/5/);
});
