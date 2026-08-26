import assert from 'node:assert/strict';
import test from 'node:test';
import { loadAddressHierarchy } from '../src/utils/checkoutAddress.js';

test('saved address loads districts then wards for the same province', async () => {
  const calls = [];
  const result = await loadAddressHierarchy({ ghnProvinceId: 201, ghnDistrictId: 1441, ghnWardCode: '26734' }, {
    getDistricts: async id => { calls.push(['districts', id]); return [{ DistrictID: 1441, DistrictName: 'Quận 1' }]; },
    getWards: async id => { calls.push(['wards', id]); return [{ WardCode: '26734', WardName: 'Bến Nghé' }]; },
  });
  assert.deepEqual(calls, [['districts', 201], ['wards', 1441]]);
  assert.equal(result.selectedDistrict, 1441);
  assert.equal(result.selectedWard, '26734');
});
