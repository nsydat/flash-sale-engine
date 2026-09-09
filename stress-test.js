import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const productId = Number(__ENV.PRODUCT_ID || 1);
const voucherCode = __ENV.VOUCHER_CODE || '';

export const options = {
  vus: Number(__ENV.VUS || 100),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'checks{type:response}': ['rate==1'],
  },
};

export default function () {
  // Stable and globally unique for the test invocation: retries use a different user intentionally.
  const userId = (__VU * 1_000_000_000) + __ITER + 1;
  const payload = { productId, userId };
  if (voucherCode) payload.voucherCode = voucherCode;

  const response = http.post(`${baseUrl}/api/v1/flash-sale/order`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { type: 'response' },
  });

  check(response, {
    'response is 202 or expected 400': (r) => r.status === 202 || r.status === 400,
    '202 includes order code': (r) => r.status !== 202 || Boolean(r.json('orderCode')),
    '400 is a controlled capacity outcome': (r) => r.status !== 400
      || ['Out of stock', 'Voucher expired', 'Voucher already redeemed', 'Invalid product or voucher']
        .includes(r.body),
  });
}
