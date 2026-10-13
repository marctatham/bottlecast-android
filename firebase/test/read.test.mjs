import assert from 'node:assert/strict';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  Timestamp, collection, documentId, getDoc, getDocs, doc, limit, orderBy, query, where,
} from 'firebase/firestore';
import {
  DAY, HOUR, admin, guest, messageId, seedBottle, seedHold, signedOut,
} from './support.mjs';

const AFLOAT = messageId(1);
const REPORTED_OUT = messageId(2);
const HELD = messageId(3);
const EXPIRED = messageId(4);
const REMOVED = messageId(5);
const PULLED = messageId(6);

function seaQuery(db, { reportCounts = [0, 1, 2], from = 0, max = 10 } = {}) {
  const constraints = [where('status', '==', 'afloat')];
  if (reportCounts) constraints.push(where('reportCount', 'in', reportCounts));
  constraints.push(where('rand', '>=', from), orderBy('rand'));
  if (max !== null) constraints.push(limit(max));
  return query(collection(db, 'messages'), ...constraints);
}

function expiredHoldsQuery(db) {
  return query(
    collection(db, 'messages'),
    where('status', '==', 'found'),
    where('reportCount', 'in', [0, 1, 2]),
    where('foundAt', '<', Timestamp.fromMillis(Date.now() - 7 * DAY - HOUR)),
    orderBy('foundAt'),
    limit(5),
  );
}

describe('reading the sea', () => {
  beforeEach(async () => {
    await seedBottle(AFLOAT, { rand: 0.3 });
    await seedBottle(REPORTED_OUT, { rand: 0.4, reportCount: 3 });
    await seedHold(HELD, 'holder', 1 * DAY);
    await seedHold(EXPIRED, 'oldfinder', 8 * DAY);
    await seedBottle(REMOVED, { status: 'removed', userId: 'alice' });
    await seedBottle(PULLED, { status: 'under_review' });
  });

  it('runs the sea query and leaves out a bottle with 3 reports', async () => {
    const snapshot = await assertSucceeds(getDocs(seaQuery(guest('finn'))));
    const ids = snapshot.docs.map((d) => d.id);
    assert.ok(ids.includes(AFLOAT));
    assert.ok(!ids.includes(REPORTED_OUT));
  });

  it('runs the wrap-around query', async () => {
    const db = guest('finn');
    const wrap = query(collection(db, 'messages'), where('status', '==', 'afloat'),
      where('reportCount', 'in', [0, 1, 2]), where('rand', '<', 0.9), orderBy('rand'), limit(10));
    await assertSucceeds(getDocs(wrap));
  });

  it('runs the expired-holds query', async () => {
    const snapshot = await assertSucceeds(getDocs(expiredHoldsQuery(guest('finn'))));
    const ids = snapshot.docs.map((d) => d.id);
    assert.ok(ids.includes(EXPIRED));
    assert.ok(!ids.includes(HELD));
  });

  it('refuses a sea query without the report filter', async () => {
    await assertFails(getDocs(seaQuery(guest('finn'), { reportCounts: null })));
  });

  it('refuses a report filter that lets 3 through', async () => {
    await assertFails(getDocs(seaQuery(guest('finn'), { reportCounts: [0, 1, 2, 3] })));
  });

  it('refuses a sea query over 20, or with no limit', async () => {
    await assertFails(getDocs(seaQuery(guest('finn'), { max: 21 })));
    await assertFails(getDocs(seaQuery(guest('finn'), { max: null })));
  });

  it('refuses querying removed or pulled bottles', async () => {
    const db = guest('finn');
    await assertFails(getDocs(query(collection(db, 'messages'), where('status', '==', 'removed'), limit(10))));
    await assertFails(getDocs(query(collection(db, 'messages'), where('status', '==', 'under_review'), limit(10))));
  });

  it("refuses 1.1.1's random query", async () => {
    const db = guest('finn');
    const old = query(collection(db, 'messages'), where(documentId(), '>=', 'k'), orderBy(documentId()), limit(10));
    await assertFails(getDocs(old));
  });

  it('refuses every read when signed out', async () => {
    await assertFails(getDoc(doc(signedOut(), 'messages', AFLOAT)));
    await assertFails(getDocs(seaQuery(signedOut())));
  });

  it('lets anyone signed in get a bottle that is afloat, held, or past its hold', async () => {
    const db = guest('finn');
    await assertSucceeds(getDoc(doc(db, 'messages', AFLOAT)));
    await assertSucceeds(getDoc(doc(db, 'messages', HELD)));
    await assertSucceeds(getDoc(doc(db, 'messages', EXPIRED)));
  });

  it('hides reported-out, removed and pulled bottles from everyone else', async () => {
    const db = guest('finn');
    await assertFails(getDoc(doc(db, 'messages', REPORTED_OUT)));
    await assertFails(getDoc(doc(db, 'messages', REMOVED)));
    await assertFails(getDoc(doc(db, 'messages', PULLED)));
  });

  it('still shows them to their writer and to admins', async () => {
    await assertSucceeds(getDoc(doc(guest('alice'), 'messages', REMOVED)));
    await assertSucceeds(getDoc(doc(guest('writer'), 'messages', REPORTED_OUT)));
    await assertSucceeds(getDoc(doc(admin(), 'messages', PULLED)));
  });

  it('lets a writer list their own bottles with no limit', async () => {
    const db = guest('alice');
    await assertSucceeds(getDocs(query(collection(db, 'messages'), where('userId', '==', 'alice'))));
  });

  it('lets anyone signed in get an id that does not exist', async () => {
    await assertSucceeds(getDoc(doc(guest('finn'), 'messages', messageId(99))));
  });

  it('lets admins list everything', async () => {
    const db = admin();
    await assertSucceeds(getDocs(collection(db, 'messages')));
    await assertSucceeds(getDocs(query(collection(db, 'messages'), where('reportCount', '>=', 1), orderBy('reportCount', 'desc'))));
  });

  it('refuses listing everything to anyone else', async () => {
    await assertFails(getDocs(collection(guest('finn'), 'messages')));
  });
});

