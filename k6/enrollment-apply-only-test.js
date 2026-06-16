import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const COURSE_OFFERING_ID = Number(__ENV.COURSE_OFFERING_ID || 4);
const VUS = Number(__ENV.VUS || 200);
const START_STUDENT_NO = Number(__ENV.START_STUDENT_NO || 100);
const PASSWORD = __ENV.PASSWORD || '1234';
const APPLY_DELAY_MS = Number(__ENV.APPLY_DELAY_MS || 30000);

const applyDuration = new Trend('apply_duration', true);
const enrolledCount = new Counter('apply_enrolled');
const waitingCount = new Counter('apply_waiting');

export const options = {
    setupTimeout: '5m',
    scenarios: {
        enrollment_apply_only: {
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

function getCookieFromResponse(res, name) {
    const values = res.cookies[name];
    if (values && values.length > 0) {
        return values[0].value;
    }
    return null;
}

function getCookieFromSetCookieHeader(res, name) {
    const setCookie = res.headers['Set-Cookie'] || res.headers['set-cookie'];
    if (!setCookie) {
        return null;
    }

    const headerValue = Array.isArray(setCookie) ? setCookie.join('; ') : setCookie;
    const match = headerValue.match(new RegExp(`${name}=([^;]+)`));
    if (!match) {
        return null;
    }

    return match[1];
}

function getCookie(res, name) {
    return getCookieFromResponse(res, name) || getCookieFromSetCookieHeader(res, name);
}

function buildCookieHeader(cookies) {
    return Object.entries(cookies)
        .filter(([, value]) => value)
        .map(([name, value]) => `${name}=${value}`)
        .join('; ');
}

function loginSession(loginId) {
    const csrfRes = http.get(`${BASE_URL}/csrf`, {
        tags: { name: 'GET /csrf' },
    });
    const csrfToken = safeDecode(getCookie(csrfRes, 'XSRF-TOKEN'));

    const loginHeaders = {
        'Content-Type': 'application/json',
    };

    if (csrfToken) {
        loginHeaders['X-XSRF-TOKEN'] = csrfToken;
        loginHeaders.Cookie = `XSRF-TOKEN=${csrfToken}`;
    }

    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            loginId,
            password: PASSWORD,
        }),
        {
            headers: loginHeaders,
            tags: { name: 'POST /auth/login' },
        }
    );

    const accessToken = getCookie(loginRes, 'ACCESS_TOKEN');
    const refreshToken = getCookie(loginRes, 'REFRESH_TOKEN');
    const loginCsrfToken = getCookie(loginRes, 'XSRF-TOKEN');
    const effectiveCsrfToken = safeDecode(loginCsrfToken || csrfToken);

    const loginOk =
        csrfRes.status >= 200 &&
        csrfRes.status < 300 &&
        loginRes.status >= 200 &&
        loginRes.status < 300 &&
        accessToken;

    if (!loginOk) {
        console.error(
            `login setup failed. loginId=${loginId}, csrfStatus=${csrfRes.status}, loginStatus=${loginRes.status}, body=${loginRes.body}`
        );
        throw new Error(`login setup failed. loginId=${loginId}`);
    }

    return {
        loginId,
        cookieHeader: buildCookieHeader({
            'XSRF-TOKEN': effectiveCsrfToken,
            ACCESS_TOKEN: accessToken,
            REFRESH_TOKEN: refreshToken,
        }),
        csrfToken: effectiveCsrfToken,
    };
}

export function setup() {
    const sessions = [];

    for (let i = 0; i < VUS; i += 1) {
        const studentNo = START_STUDENT_NO + i;
        const loginId = `stu${paddedStudentNo(studentNo)}`;
        sessions.push(loginSession(loginId));
    }

    return {
        applyAt: Date.now() + APPLY_DELAY_MS,
        sessions,
    };
}

function waitUntilApplyTime(applyAt) {
    const waitMs = applyAt - Date.now();
    if (waitMs > 0) {
        sleep(waitMs / 1000);
    }
}

export default function (data) {
    const session = data.sessions[__VU - 1];
    if (!session) {
        throw new Error(`session not found. vu=${__VU}`);
    }

    waitUntilApplyTime(data.applyAt);

    const res = http.post(
        `${BASE_URL}/student/enrollments`,
        JSON.stringify({
            courseOfferingId: COURSE_OFFERING_ID,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: session.cookieHeader,
                'X-XSRF-TOKEN': session.csrfToken,
            },
            tags: { name: 'POST /student/enrollments' },
        }
    );

    applyDuration.add(res.timings.duration);

    let body = null;
    try {
        body = res.json();
    } catch (e) {
        body = null;
    }

    const applyOk = check(res, {
        'apply status is 2xx': (r) => r.status >= 200 && r.status < 300,
        'apply result is ENROLLED or WAITING': () =>
            body && (body.status === 'ENROLLED' || body.status === 'WAITING'),
    });

    if (body && body.status === 'ENROLLED') {
        enrolledCount.add(1);
    }

    if (body && body.status === 'WAITING') {
        waitingCount.add(1);
    }

    if (!applyOk) {
        console.error(
            `apply failed. loginId=${session.loginId}, status=${res.status}, body=${res.body}`
        );
    }
}
