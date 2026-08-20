import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const homeUrl = new URL('../src/views/guest/HomePage.vue', import.meta.url);

test('home tells the approved daily-meal story with a menu CTA', async () => {
  const source = await readFile(homeUrl, 'utf8');
  const story = source.slice(source.indexOf('<section class="story-section"'), source.indexOf('<section class="experience-section"'));

  assert.match(story, /Cuộc sống có thể vội\.[\s\S]*Bữa ăn thì không\./);
  assert.match(story, /Một bữa ăn nhỏ, một ngày dài/i);
  assert.match(story, /<router-link to="\/menu" class="story-cta">Chọn bữa ăn của bạn/);
  assert.match(story, /res\.cloudinary\.com/);
  assert.match(story, /alt="Người trẻ dùng bữa khi làm việc trên laptop"/);
});

test('home keeps one concise three-step ordering guide and removes unsupported metrics', async () => {
  const source = await readFile(homeUrl, 'utf8');
  const guide = source.slice(source.indexOf('<section class="experience-section"'), source.indexOf('<section class="home-cta"'));

  assert.equal((guide.match(/<router-link class="experience-card"/g) || []).length, 3);
  assert.match(guide, /to="\/menu"[\s\S]*<h3>Chọn món<\/h3>[\s\S]*to="\/cart"[\s\S]*<h3>Kiểm tra giỏ<\/h3>[\s\S]*to="\/track-order"[\s\S]*<h3>Theo dõi đơn<\/h3>/);
  assert.doesNotMatch(guide, /Mở thực đơn|Mở giỏ hàng|>Theo dõi đơn<\/router-link>/);
  assert.match(source, /\.experience-card:hover>i:not\(\.card-arrow\),[\s\S]*\.experience-card:focus-visible>i:not\(\.card-arrow\)/);
  assert.doesNotMatch(source, /class="about-section"/);
  assert.doesNotMatch(source, /10,000\+|4\.8<small>\/5/);
});
