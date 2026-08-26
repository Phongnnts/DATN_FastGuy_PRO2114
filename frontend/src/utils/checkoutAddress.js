export async function loadAddressHierarchy(address, shippingApi) {
  const provinceId = Number(address?.ghnProvinceId) || null;
  const districtId = Number(address?.ghnDistrictId) || null;
  const wardCode = address?.ghnWardCode == null ? null : String(address.ghnWardCode);
  if (!provinceId || !districtId || !wardCode) throw new Error('Địa chỉ chưa có đủ mã GHN');

  const districtData = await shippingApi.getDistricts(provinceId);
  const districts = (districtData || []).map(d => ({
    id: d.DistrictID || d.district_id || d.districtId,
    name: d.DistrictName || d.district_name || d.districtName,
  }));
  if (!districts.some(d => Number(d.id) === districtId)) throw new Error('Quận/huyện GHN không còn khả dụng');

  const wardData = await shippingApi.getWards(districtId);
  const wards = (wardData || []).map(w => ({
    code: String(w.WardCode || w.ward_code || w.wardCode),
    name: w.WardName || w.ward_name || w.wardName,
  }));
  if (!wards.some(w => w.code === wardCode)) throw new Error('Phường/xã GHN không còn khả dụng');
  return { provinceId, districts, wards, selectedDistrict: districtId, selectedWard: wardCode };
}
