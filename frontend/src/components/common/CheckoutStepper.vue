<script setup>
const props = defineProps({ current: { type: Number, required: true, validator: value => value >= 1 && value <= 4 } });
const steps = [
  { number: 1, label: 'Giỏ hàng', icon: 'bi-bag-check' },
  { number: 2, label: 'Thông tin giao', icon: 'bi-geo-alt' },
  { number: 3, label: 'Thanh toán', icon: 'bi-credit-card' },
  { number: 4, label: 'Hoàn tất', icon: 'bi-check2-circle' },
];
</script>

<template>
  <ol class="checkout-flow" aria-label="Tiến trình đặt hàng">
    <li v-for="step in steps" :key="step.number" class="flow-step" :class="{ active: step.number === props.current, complete: step.number < props.current }" :aria-current="step.number === props.current ? 'step' : undefined">
      <span class="step-marker"><i v-if="step.number <= props.current" :class="`bi ${step.number < props.current ? 'bi-check-lg' : step.icon}`" aria-hidden="true"></i><b v-else>{{ step.number }}</b></span>
      <span class="step-copy"><small>Bước {{ step.number }}</small><strong>{{ step.label }}</strong></span>
    </li>
  </ol>
</template>

<style scoped>
.checkout-flow{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));margin:0 0 20px;padding:18px 22px;border:1px solid #ece4de;border-radius:18px;background:#fff;box-shadow:0 10px 28px rgba(40,27,20,.045);list-style:none}.flow-step{position:relative;display:flex;align-items:center;gap:10px;min-width:0;color:#9b8b82}.flow-step:not(:last-child)::after{position:absolute;top:21px;right:14px;left:62px;height:1px;background:#e8dfda;content:""}.step-marker{position:relative;z-index:1;display:grid;width:42px;height:42px;flex:0 0 42px;place-items:center;border-radius:50%;color:#81736b;background:#f1f3f7;font-size:13px;font-weight:850}.step-copy small,.step-copy strong{display:block}.step-copy small{margin-bottom:2px;font-size:9px;font-weight:800;letter-spacing:.08em;text-transform:uppercase}.step-copy strong{font-size:12px;white-space:nowrap}.flow-step.active{color:#b84721}.flow-step.active .step-marker,.flow-step.complete .step-marker{color:#fff;background:#df683e;box-shadow:0 8px 18px rgba(223,104,62,.22)}.flow-step.complete::after{background:#df683e}@media(max-width:680px){.checkout-flow{padding:14px 8px}.flow-step{justify-content:center}.step-copy small{display:none}.step-copy strong{margin-top:5px;font-size:9px;white-space:normal;text-align:center}.flow-step{flex-direction:column;gap:2px}.flow-step:not(:last-child)::after{top:21px;right:-25%;left:66%}.step-marker{width:40px;height:40px;flex-basis:40px}}@media(max-width:390px){.step-copy strong{font-size:8px}}
</style>
