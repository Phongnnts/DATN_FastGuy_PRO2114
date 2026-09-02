<script setup>
import axios from 'axios';
import { ref, watch } from 'vue';
import { sectionDirty } from '@/utils/adminProductEditor';
import { CLOUDINARY as CLOUDINARY_CONFIG } from '@/utils/constants';

const props = defineProps({ modelValue: { type: Object, required: true }, busy: Boolean, baselineVersion: { type: Number, required: true } });
const emit = defineEmits(['update:modelValue', 'save', 'dirty-change']);
const uploading = ref(false);
const uploadError = ref('');
const snapshot = ref({ image: props.modelValue.image, galleryImages: [...props.modelValue.galleryImages] });

watch(() => props.baselineVersion, () => {
  snapshot.value = { image: props.modelValue.image, galleryImages: [...props.modelValue.galleryImages] };
  emit('dirty-change', false);
});
watch(() => [props.modelValue.image, props.modelValue.galleryImages], () => {
  emit('dirty-change', sectionDirty(snapshot.value, { image: props.modelValue.image, galleryImages: props.modelValue.galleryImages }));
}, { deep: true });

function update(values) {
  emit('update:modelValue', { ...props.modelValue, ...values });
}

async function upload(event, gallery = false) {
  const file = event.target.files?.[0];
  if (!file || props.busy || uploading.value) return;
  uploading.value = true;
  uploadError.value = '';
  try {
    const data = new FormData();
    data.append('file', file);
    data.append('upload_preset', CLOUDINARY_CONFIG.uploadPreset);
    const response = await axios.post(CLOUDINARY_CONFIG.uploadUrl, data);
    if (typeof response.data?.secure_url !== 'string' || !response.data.secure_url) throw new Error('Cloudinary không trả về URL ảnh hợp lệ');
    const image = response.data.secure_url;
    update(gallery ? { galleryImages: [...props.modelValue.galleryImages, image] } : { image });
  } catch (error) {
    uploadError.value = error.message || 'Không thể tải ảnh lên';
  } finally {
    uploading.value = false;
    event.target.value = '';
  }
}

function removeImage(index) {
  update({ galleryImages: props.modelValue.galleryImages.filter((_, imageIndex) => imageIndex !== index) });
}

</script>

<template>
  <section class="editor-card" aria-labelledby="media-title">
    <div>
      <h2 id="media-title">Hình ảnh sản phẩm</h2>
      <p class="section-helper">Chọn ảnh đại diện rõ món ăn và thêm ảnh thư viện khi cần.</p>
    </div>
    <div class="upload-block">
      <label for="primary-image">Ảnh đại diện</label>
      <input id="primary-image" type="file" accept="image/*" :disabled="busy || uploading" aria-describedby="primary-image-helper" @change="upload($event)" />
      <span id="primary-image-helper" class="field-helper">Ảnh sẽ được tải lên trước khi lưu sản phẩm.</span>
      <img v-if="modelValue.image" class="preview primary" :src="modelValue.image" alt="Xem trước ảnh đại diện" />
      <button v-if="modelValue.image" class="btn btn-outline" type="button" :disabled="busy || uploading" @click="update({ image: '' })">Xóa ảnh đại diện</button>
    </div>
    <div class="upload-block">
      <label for="gallery-image">Thêm ảnh thư viện</label>
      <input id="gallery-image" type="file" accept="image/*" :disabled="busy || uploading" @change="upload($event, true)" />
      <ul v-if="modelValue.galleryImages.length" class="gallery" aria-label="Thư viện ảnh">
        <li v-for="(image, index) in modelValue.galleryImages" :key="`${image}-${index}`"><img class="preview" :src="image" :alt="`Ảnh thư viện ${index + 1}`" /><button type="button" :disabled="busy || uploading" :aria-label="`Xóa ảnh thư viện ${index + 1}`" @click="removeImage(index)">×</button></li>
      </ul>
    </div>
    <p v-if="uploading" role="status">Đang tải ảnh...</p>
    <p v-if="uploadError" role="alert">{{ uploadError }}</p>
    <div class="actions"><button class="btn btn-primary" type="button" :disabled="busy || uploading" @click="emit('save')">{{ busy ? 'Đang lưu...' : 'Lưu hình ảnh' }}</button></div>
  </section>
</template>

<style scoped>
.editor-card{display:grid;gap:22px;padding:24px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff}.editor-card h2{margin:0}.section-helper,.field-helper{margin:5px 0 0;color:var(--text-mid);font-size:13px}.upload-block{display:grid;gap:10px}.upload-block label{font-size:12px;font-weight:700}.primary{width:min(320px,100%)}.preview{height:120px;max-width:100%;border-radius:12px;object-fit:cover}.gallery{display:flex;flex-wrap:wrap;gap:12px;margin:0;padding:0;list-style:none}.gallery li{position:relative}.gallery button{position:absolute;top:5px;right:5px;width:30px;height:30px;border:0;border-radius:50%;color:#fff;background:#991b1b}.actions{display:flex;justify-content:flex-end}[role=alert]{color:#b91c1c}
</style>
