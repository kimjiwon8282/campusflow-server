import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const COURSE_OFFERING_ID = Number(__ENV.COURSE_OFFERING_ID || 4);
const VUS = Number(__ENV.VUS || 20);
const START_STUDENT_NO = Number(__ENV.START_STUDENT_NO || 100);
const PASSWORD = __ENV.PASSWORD || '1234';
const APPLY_DELAY_MS = Number(__ENV.APPLY_DELAY_MS || 10000);

const applyDuration = new Trend('apply_duration', true);
const enrolledCount = new Counter('apply_enrolled');
const waitingCount = new Counter('apply_waiting');

export const options = {
    scenarios: {
        enrollment_contention: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1,
            maxDuration: '2m',
        },
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    thresholds: {
        http_req_failed: ['rate<0.05'],
    },
};

export function setup() {
    return {
        applyAt: Date.now() + APPLY_DELAY_MS,
    };
}

function paddedStudentNo(number) {
    return String(number).padStart(4, '0');
}

function safeDecode(value) {
    try {
        return decodeURIComponent(value);
    } catch (e) {
        return value;
    }
}

function getCookieFromJar(name) {
    const jar = http.cookieJar();
    const cookies = jar.cookiesForURL(BASE_URL);
    const values = cookies[name];

    if (values && values.length > 0) {
        return values[0];
    }

    return null;
}

function getCookieFromResponse(res, name) {
    const values = res.cookies[name];

    if (values && values.length > 0) {
        return values[0].value;
    }

    return null;
}

function getCookieFromSetCookieHeader(res, name) {
    const setCookie =
        res.headers['Set-Cookie'] ||
        res.headers['set-cookie'];

    if (!setCookie) {
        return null;
    }

    const match = setCookie.match(new RegExp(`${name}=([^;]+)`));

    if (!match) {
        return null;
    }

    return match[1];
}

function ensureCsrfToken() {
    let rawToken = getCookieFromJar('XSRF-TOKEN');

    if (rawToken) {
        return safeDecode(rawToken);
    }

    const res = http.get(`${BASE_URL}/csrf`, {
        tags: { name: 'GET /csrf' },
    });

    check(res, {
        'csrf status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });

    rawToken =
        getCookieFromJar('XSRF-TOKEN') ||
        getCookieFromResponse(res, 'XSRF-TOKEN') ||
        getCookieFromSetCookieHeader(res, 'XSRF-TOKEN');

    if (rawToken) {
        const jar = http.cookieJar();
        jar.set(BASE_URL, 'XSRF-TOKEN', rawToken, { path: '/' });
        return safeDecode(rawToken);
    }

    console.error(
        `XSRF-TOKEN cookie not found. status=${res.status}, body=${res.body}, setCookie=${res.headers['Set-Cookie']}`
    );

    return null;
}

function csrfHeaders() {
    const csrfToken = ensureCsrfToken();

    if (!csrfToken) {
        return {
            'Content-Type': 'application/json',
        };
    }

    return {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken,
    };
}

function login(loginId) {
    return http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            loginId,
            password: PASSWORD,
        }),
        {
            headers: csrfHeaders(),
            tags: { name: 'POST /auth/login' },
        }
    );
}

function waitUntilApplyTime(applyAt) {
    const waitMs = applyAt - Date.now();

    if (waitMs > 0) {
        sleep(waitMs / 1000);
    }
}

function applyEnrollment() {
    const startedAt = Date.now();

    const res = http.post(
        `${BASE_URL}/student/enrollments`,
        JSON.stringify({
            courseOfferingId: COURSE_OFFERING_ID,
        }),
        {
            headers: csrfHeaders(),
            tags: { name: 'POST /student/enrollments' },
        }
    );

    applyDuration.add(Date.now() - startedAt);

    let data = null;
    try {
        data = res.json();
    } catch (e) {
        // JSON 파싱 실패 시 data는 null 유지
    }

    check(res, {
        'apply status is 2xx': (r) => r.status >= 200 && r.status < 300,
        'apply result is ENROLLED or WAITING': () =>
            data && (data.status === 'ENROLLED' || data.status === 'WAITING'),
    });

    if (data && data.status === 'ENROLLED') {
        enrolledCount.add(1);
    }

    if (data && data.status === 'WAITING') {
        waitingCount.add(1);
    }

    if (res.status < 200 || res.status >= 300) {
        console.error(`apply failed. status=${res.status}, body=${res.body}`);
    }
}

export default function (data) {
    const studentNo = START_STUDENT_NO + __VU - 1;
    const loginId = `stu${paddedStudentNo(studentNo)}`;

    const loginRes = login(loginId);

    const loginOk = check(loginRes, {
        'login status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });

    if (!loginOk) {
        console.error(
            `login failed. loginId=${loginId}, status=${loginRes.status}, body=${loginRes.body}`
        );
        return;
    }

    waitUntilApplyTime(data.applyAt);
    applyEnrollment();
}