// k6 load test for the strangler gateway. Basis for the p99 latency story.
//
// Run against the gateway (which weight-routes /api/orders between monolith and orders-service):
//   k6 run loadtest/orders.js
//
// Drive the canary from another shell while this runs to compare p99 across weights:
//   curl -XPOST localhost:8080/admin/canary/weight/0     # 100% monolith  (baseline)
//   curl -XPOST localhost:8080/admin/canary/weight/50    # 50/50 split
//   curl -XPOST localhost:8080/admin/canary/weight/100   # 100% orders-service
//
// NOTE: numbers from this script are LOCAL load-test figures, not production SLOs.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.GATEWAY_URL || 'http://localhost:8080';

// Separate latency trend so you can read p99 per backend from the X-Order-Backend response header.
const readLatency = new Trend('order_read_latency', true);

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    // The p99 story: assert the read path stays well under 250ms at this load.
    'order_read_latency': ['p(99)<250'],
    'http_req_failed': ['rate<0.01'],
  },
};

// Seed one customer + order so reads have something to hit.
export function setup() {
  const customer = http.post(`${BASE}/api/customers`, JSON.stringify({
    name: 'Load Test', email: `load-${Date.now()}@example.com`,
  }), { headers: { 'Content-Type': 'application/json' } });
  const customerId = customer.json('id');

  const order = http.post(`${BASE}/api/orders`, JSON.stringify({
    customerId: customerId, totalAmount: 199.99,
  }), { headers: { 'Content-Type': 'application/json' } });

  return { orderId: order.json('id') };
}

export default function (data) {
  const res = http.get(`${BASE}/api/orders/${data.orderId}`);
  readLatency.add(res.timings.duration, { backend: res.headers['X-Order-Backend'] || 'unknown' });
  check(res, {
    'status is 200': (r) => r.status === 200,
    'has order id': (r) => r.json('id') === data.orderId,
  });
  sleep(0.2);
}
