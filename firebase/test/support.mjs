// Shared helpers for the rules tests: the test environment, who's asking, seed data and the
// writes the app makes, each shaped exactly as the app sends it.
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import {
  Timestamp, doc, increment, serverTimestamp, setDoc, setLogLevel, writeBatch,
} from 'firebase/firestore';

// Denied writes are the point of most tests; keep the SDK from logging each one.
setLogLevel('silent');

export const DAY = 24 * 60 * 60 * 1000;
export const HOUR = 60 * 60 * 1000;

let testEnv;

export async function setUpEnvironment() {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-bottlecast',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
}

export async function tearDownEnvironment() {
  await testEnv?.cleanup();
}

export async function clearData() {
  await testEnv.clearFirestore();
}

// ---------- Who is asking ----------

const googleToken = { firebase: { sign_in_provider: 'google.com', identities: { 'google.com': ['1'] } } };

export function guest(uid) {
  return testEnv.authenticatedContext(uid, { firebase: { sign_in_provider: 'anonymous', identities: {} } })
    .firestore();
}

export function google(uid) {
  return testEnv.authenticatedContext(uid, googleToken).firestore();
}

export function admin(uid = 'moderator') {
  return testEnv.authenticatedContext(uid, { ...googleToken, admin: true }).firestore();
}

export function withToken(uid, token) {
  return testEnv.authenticatedContext(uid, token).firestore();
}

export function signedOut() {
  return testEnv.unauthenticatedContext().firestore();
}

// ---------- Seed data (written with the rules off) ----------

export async function seed(write) {
  await testEnv.withSecurityRulesDisabled(async (context) => write(context.firestore()));
}

// Message ids are 20 alphanumerics, as Firestore's auto ids are.
export function messageId(n) {
  return `bottle${String(n).padStart(14, '0')}`;
}

export const M1 = messageId(1);
export const M2 = messageId(2);

export const WRITER = 'writer';

export function bottle(id, overrides = {}) {
  return {
    id,
    messageContent: 'Leave the porch light on for the people still finding their way home.',
    userId: WRITER,
    createdAt: Timestamp.fromMillis(Date.now() - 3 * DAY),
    lang: 'und',
    status: 'afloat',
    rand: 0.5,
    penName: null,
    sealable: false,
    reportCount: 0,
    findCount: 0,
    foundAt: null,
    ...overrides,
  };
}

export async function seedBottle(id, overrides = {}) {
  await seed((db) => setDoc(doc(db, 'messages', id), bottle(id, overrides)));
}

// A hold by `finderUid`, `ageMillis` old: the message, the finder's find and their daily record,
// all stamped with the same time, as one find commit would leave them.
export async function seedHold(id, finderUid, ageMillis, { findStatus = 'held', reportCount = 0, findCount = 1 } = {}) {
  const foundAt = Timestamp.fromMillis(Date.now() - ageMillis);
  const date = utcDate(Date.now() - ageMillis);
  await seed(async (db) => {
    await setDoc(doc(db, 'messages', id), bottle(id, { status: 'found', foundAt, findCount, reportCount }));
    await setDoc(doc(db, 'finds', `${id}_${finderUid}`), {
      messageId: id, finderUid, foundAt, dailyId: `${finderUid}_${date}`, status: findStatus,
    });
    await setDoc(doc(db, 'daily', `${finderUid}_${date}`), {
      uid: finderUid, date, zone: 'UTC', messageId: id, foundAt,
    });
  });
  return foundAt;
}

// ---------- Dates ----------

export function utcDate(millis = Date.now()) {
  return new Date(millis).toISOString().slice(0, 10);
}

export function utcHour() {
  return new Date().getUTCHours();
}

// ---------- The app's writes ----------

export function castFields(uid, id, overrides = {}) {
  return {
    id,
    messageContent: 'Hey, that is pretty cool. Drink some water today.',
    userId: uid,
    createdAt: serverTimestamp(),
    lang: 'und',
    status: 'afloat',
    rand: 0.25,
    penName: null,
    sealable: false,
    reportCount: 0,
    findCount: 0,
    foundAt: null,
    ...overrides,
  };
}

export function cast(db, uid, id, overrides = {}) {
  return setDoc(doc(db, 'messages', id), castFields(uid, id, overrides));
}

// The pop, as one commit: hold the bottle, create my find and today's daily record.
export function findWrites(uid, id, {
  date = utcDate(), zone = 'UTC', findCountBefore = 0, message = {}, find = {}, daily = {},
} = {}) {
  return [
    ['update', ['messages', id], { status: 'found', findCount: findCountBefore + 1, foundAt: serverTimestamp(), ...message }],
    ['set', ['finds', `${id}_${uid}`], {
      messageId: id, finderUid: uid, foundAt: serverTimestamp(), dailyId: `${uid}_${date}`, status: 'held', ...find,
    }],
    ['set', ['daily', `${uid}_${date}`], { uid, date, zone, messageId: id, foundAt: serverTimestamp(), ...daily }],
  ];
}

export function throwBackWrites(uid, id) {
  return [
    ['update', ['finds', `${id}_${uid}`], { status: 'thrown_back' }],
    ['update', ['messages', id], { status: 'afloat', foundAt: null }],
  ];
}

export function reportWrites(uid, id, { reason = null, blockedWriter = false, report = {} } = {}) {
  return [
    ['set', ['reports', `${id}_${uid}`], {
      messageId: id, reporterUid: uid, reason, blockedWriter, createdAt: serverTimestamp(), ...report,
    }],
    ['update', ['messages', id], { reportCount: increment(1) }],
    ['update', ['finds', `${id}_${uid}`], { status: 'reported' }],
  ];
}

// Commits some writes (or a subset, by index) as one batch.
export function commit(db, writes, only) {
  const batch = writeBatch(db);
  writes.forEach(([kind, path, data], index) => {
    if (only && !only.includes(index)) return;
    const ref = doc(db, ...path);
    if (kind === 'set') batch.set(ref, data);
    else batch.update(ref, data);
  });
  return batch.commit();
}
