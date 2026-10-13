import assert from 'node:assert/strict';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  Timestamp, collection, doc, getDoc, getDocs, increment, limit, orderBy, query, setDoc, where,
} from 'firebase/firestore';
import {
  DAY, HOUR, M1, admin, commit, guest, reportWrites, seed, seedBottle, seedHold, throwBackWrites, utcDate,
} from './support.mjs';

async function reportCountOf(id) {
  let count;
  await seed(async (db) => {
    count = (await getDoc(doc(db, 'messages', id))).data().reportCount;
  });
  return count;
}

describe('reporting a bottle', () => {
  for (const reason of [null, 'hurtful', 'sexual', 'personal_info', 'spam', 'other']) {
    it(`lets the live holder report it, with reason ${reason}`, async () => {
      await seedHold(M1, 'finn', HOUR);
      await assertSucceeds(commit(guest('finn'), reportWrites('finn', M1, { reason })));
    });
  }

  it('lets a report also say the writer was blocked', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertSucceeds(commit(guest('finn'), reportWrites('finn', M1, { blockedWriter: true })));
  });

  it('counts the report on the server', async () => {
    await seedHold(M1, 'finn', HOUR, { reportCount: 1 });
    await assertSucceeds(commit(guest('finn'), reportWrites('finn', M1)));
    assert.equal(await reportCountOf(M1), 2);
  });

  it('takes the bottle out of the sea at its third report', async () => {
    await seedHold(M1, 'finn', HOUR, { reportCount: 2 });
    await assertSucceeds(commit(guest('finn'), reportWrites('finn', M1)));

    const other = guest('fern');
    const held = await getDocs(query(collection(other, 'messages'), where('status', '==', 'found'),
      where('reportCount', 'in', [0, 1, 2]), orderBy('foundAt'), limit(5)));
    assert.ok(!held.docs.some((d) => d.id === M1));
    await assertFails(getDoc(doc(other, 'messages', M1)));
    await assertSucceeds(getDoc(doc(guest('writer'), 'messages', M1)));
    await assertSucceeds(getDoc(doc(admin(), 'messages', M1)));
  });

  it('lets reporters read their own report', async () => {
    await seedHold(M1, 'finn', HOUR);
    await commit(guest('finn'), reportWrites('finn', M1));
    await assertSucceeds(getDoc(doc(guest('finn'), 'reports', `${M1}_finn`)));
  });

  for (const [label, report] of [
    ['an unknown reason', { reason: 'boring' }],
    ['a block flag that is not a boolean', { blockedWriter: 'yes' }],
    ["the client's own createdAt", { createdAt: Timestamp.now() }],
    ['someone else as the reporter', { reporterUid: 'fern' }],
    ['another bottle', { messageId: 'bottle00000000000099' }],
    ['an extra key', { note: 'rude' }],
  ]) {
    it(`refuses ${label}`, async () => {
      await seedHold(M1, 'finn', HOUR);
      await assertFails(commit(guest('finn'), reportWrites('finn', M1, { report })));
    });
  }

  for (const only of [[0], [1], [2], [0, 1], [0, 2], [1, 2]]) {
    it(`refuses part of the commit on its own (${only})`, async () => {
      await seedHold(M1, 'finn', HOUR);
      await assertFails(commit(guest('finn'), reportWrites('finn', M1), only));
    });
  }

  it('refuses counting two reports at once, or changing the status with it', async () => {
    await seedHold(M1, 'finn', HOUR);
    const plusTwo = reportWrites('finn', M1);
    plusTwo[1][2] = { reportCount: increment(2) };
    await assertFails(commit(guest('finn'), plusTwo));
    const withStatus = reportWrites('finn', M1);
    withStatus[1][2] = { reportCount: increment(1), status: 'under_review' };
    await assertFails(commit(guest('finn'), withStatus));
  });

  it('refuses a second report', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertSucceeds(commit(guest('finn'), reportWrites('finn', M1)));
    await assertFails(commit(guest('finn'), reportWrites('finn', M1)));
  });

  it('refuses someone who never found it', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertFails(commit(guest('fern'), reportWrites('fern', M1)));
  });

  it('refuses a finder who threw it back', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertSucceeds(commit(guest('finn'), throwBackWrites('finn', M1)));
    await assertFails(commit(guest('finn'), reportWrites('finn', M1)));
  });

  it('refuses a report once the hold has expired', async () => {
    await seedHold(M1, 'finn', 8 * DAY);
    await assertFails(commit(guest('finn'), reportWrites('finn', M1)));
  });

  it('refuses an old finder once someone else holds it', async () => {
    const old = Timestamp.fromMillis(Date.now() - 9 * DAY);
    await seedHold(M1, 'second', HOUR, { findCount: 2 });
    await seed((db) => setDoc(doc(db, 'finds', `${M1}_first`), {
      messageId: M1, finderUid: 'first', foundAt: old, dailyId: `first_${utcDate(Date.now() - 9 * DAY)}`, status: 'held',
    }));
    await assertFails(commit(guest('first'), reportWrites('first', M1)));
  });

  it("refuses reading someone else's report, or listing reports", async () => {
    await seedHold(M1, 'finn', HOUR);
    await commit(guest('finn'), reportWrites('finn', M1));
    await assertFails(getDoc(doc(guest('fern'), 'reports', `${M1}_finn`)));
    await assertFails(getDocs(collection(guest('finn'), 'reports')));
    await assertSucceeds(getDocs(collection(admin(), 'reports')));
  });

  it("refuses reporting a bottle that's afloat", async () => {
    await seedBottle(M1);
    await assertFails(commit(guest('finn'), reportWrites('finn', M1)));
  });
});
