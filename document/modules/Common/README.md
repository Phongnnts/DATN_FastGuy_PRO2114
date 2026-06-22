# Module Common

**Mục tiêu**: Layouts, styles, utils, components dùng chung, Cloudinary config, Chart.js.

**Người phụ trách**: Người 6

---

## Files

### Layouts
- `layouts/GuestLayout.vue`
- `layouts/UserLayout.vue`
- `layouts/StaffLayout.vue`
- `layouts/AdminLayout.vue`

### Styles
- `assets/styles/global.css`
- `assets/styles/variables.css`

### Utils
- `utils/constants.js`
- `utils/format.js`
- `utils/helpers.js`
- `utils/validators.js`

### Components
- `components/common/ProductCard.vue`
- `components/common/HeroBanner.vue`

### Router
- `router/index.js`

### Backend chung
- `utils/DatabaseUtil.java`
- `utils/JsonUtil.java`
- `utils/ApiResponse.java`
- `entity/*.java` (17 entities)

---

## Cloudinary Config (thêm vào constants.js)

```js
CLOUDINARY: {
  cloudName: 'ds4dnsj0o',
  uploadPreset: 'upload-fastguy',
  uploadUrl: 'https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload',
  folders: [
    'Image_Cloudinery/Burger/',
    'Image_Cloudinery/GaRan/',
    'Image_Cloudinery/Tacos/',
    'Image_Cloudinery/Pizza/',
    'Image_Cloudinery/Com/',
    'Image_Cloudinery/BanhMi/',
    'Image_Cloudinery/GoiTom/',
    'Image_Cloudinery/KhoaiTay/',
    'Image_Cloudinery/Nuoc/',
  ],
}
```

---

## Việc cần làm

- [ ] Frontend: thêm Cloudinary config vào `constants.js`
- [ ] Frontend: kiểm tra Chart.js admin dashboard
- [ ] Frontend: kiểm tra Chart.js staff dashboard
- [ ] Frontend: rà soát layout + sidebar links
- [ ] Hỗ trợ các module khác khi cần

---

## Phụ thuộc

- **Không phụ thuộc module nào**
- Tất cả module khác phụ thuộc Common
