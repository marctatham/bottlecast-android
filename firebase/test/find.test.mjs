import assert from 'node:assert/strict';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  Timestamp, collection, doc, getDoc, getDocs, query, runTransaction, serverTimestamp, setDoc, where,
} from 'firebase/firestore';
import {
  DAY, HOUR, M1, M2, commit, findWrites, guest, seed, seedBottle, seedHold, utcDate, utcHour,
} from './support.mjs';

describe("finding today's bottle", () => {
  it('lets a finder hold a bottle in one commit', async () => {
    await seedBottle(M1);
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M1)));
  });

  it('works as the transaction the app runs, reading my missing find and daily record first', async () => {
    await seedBottle(M1);
    const db = guest('finn');
    const date = utcDate();
    await assertSucceeds(runTransaction(db, async (tx) => {
      const message = await tx.get(doc(db, 'messages', M1));
      const find = await tx.get(doc(db, 'finds', `${M1}_finn`));
      const daily = await tx.get(doc(db, 'daily', `finn_${date}`));
      assert.ok(message.exists() && !find.exists() && !daily.exists());
      tx.update(doc(db, 'messages', M1), { status: 'found', findCount: message.data().findCount + 1, foundAt: serverTimestamp() });
      tx.set(doc(db, 'finds', `${M1}_finn`), {
        messageId: M1, finderUid: 'finn', foundAt: serverTimestamp(), dailyId: `finn_${date}`, status: 'held',
      });
      tx.set(doc(db, 'daily', `finn_${date}`), { uid: 'finn', date, zone: 'Europe/London', messageId: M1, foundAt: serverTimestamp() });
    }));
  });

  it('lets someone else take a hold that has expired', async () => {
    await seedHold(M1, 'first', 8 * DAY);
    await assertSucceeds(commit(guest('second'), findWrites('second', M1, { findCountBefore: 1 })));
  });

  it('refuses a live hold', async () => {
    await seedHold(M1, 'first', 1 * DAY);
    await assertFails(commit(guest('second'), findWrites('second', M1, { findCountBefore: 1 })));
  });

  it('refuses my own bottle', async () => {
    await seedBottle(M1, { userId: 'finn' });
    await assertFails(commit(guest('finn'), findWrites('finn', M1)));
  });

  for (const [label, overrides] of [
    ['3 reports', { reportCount: 3 }], ['removed', { status: 'removed' }], ['pulled', { status: 'under_review' }],
  ]) {
    it(`refuses a bottle that is ${label}`, async () => {
      await seedBottle(M1, overrides);
      await assertFails(commit(guest('finn'), findWrites('finn', M1)));
    });
  }

  for (const only of [[0], [1], [2], [0, 1], [0, 2], [1, 2]]) {
    it(`refuses part of the commit on its own (${only})`, async () => {
      await seedBottle(M1);
      await assertFails(commit(guest('finn'), findWrites('finn', M1), only));
    });
  }

  it('refuses a second find on the same date', async () => {
    await seedBottle(M1);
    await seedBottle(M2);
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M1)));
    await assertFails(commit(guest('finn'), findWrites('finn', M2)));
  });

  it('refuses a bottle I found before, now back in the sea', async () => {
    const earlier = Timestamp.fromMillis(Date.now() - 9 * DAY);
    await seedBottle(M1, { findCount: 1 });
    await seed((db) => setDoc(doc(db, 'finds', `${M1}_finn`), {
      messageId: M1, finderUid: 'finn', foundAt: earlier, dailyId: `finn_${utcDate(Date.now() - 9 * DAY)}`, status: 'thrown_back',
    }));
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { findCountBefore: 1 })));
  });

  for (const [label, message] of [
    ['a find count of +2', { findCount: 2 }],
    ['an unchanged find count', { findCount: 0 }],
    ["the client's own foundAt", { foundAt: Timestamp.now() }],
    ['a changed rand', { rand: 0.9 }],
    ['a changed report count', { reportCount: 1 }],
    ['changed text', { messageContent: 'Something else entirely, now.' }],
    ['a changed writer', { userId: 'finn' }],
  ]) {
    it(`refuses ${label} on the message`, async () => {
      await seedBottle(M1);
      await assertFails(commit(guest('finn'), findWrites('finn', M1, { message })));
    });
  }

  it("refuses someone else's daily record", async () => {
    await seedBottle(M1);
    const date = utcDate();
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { find: { dailyId: `other_${date}` } })));
  });

  it('refuses a daily record that names another bottle', async () => {
    await seedBottle(M1);
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { daily: { messageId: M2 } })));
  });

  it('refuses a find created as thrown back', async () => {
    await seedBottle(M1);
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { find: { status: 'thrown_back' } })));
  });

  it('refuses an extra key on the find or the daily record', async () => {
    await seedBottle(M1);
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { find: { note: 'x' } })));
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { daily: { note: 'x' } })));
  });

  it('lets me read my own find and daily record, even before they exist', async () => {
    const db = guest('finn');
    await assertSucceeds(getDoc(doc(db, 'finds', `${M1}_finn`)));
    await assertSucceeds(getDoc(doc(db, 'daily', `finn_${utcDate()}`)));
    await assertSucceeds(getDocs(query(collection(db, 'finds'), where('finderUid', '==', 'finn'))));
    await assertSucceeds(getDocs(query(collection(db, 'daily'), where('uid', '==', 'finn'))));
  });

  it("refuses reading someone else's finds and daily records", async () => {
    await seedHold(M1, 'holder', HOUR);
    const db = guest('finn');
    await assertFails(getDoc(doc(db, 'finds', `${M1}_holder`)));
    await assertFails(getDoc(doc(db, 'daily', `holder_${utcDate(Date.now() - HOUR)}`)));
    await assertFails(getDocs(query(collection(db, 'finds'), where('finderUid', '==', 'holder'))));
    await assertFails(getDocs(collection(db, 'daily')));
  });

  it('lets exactly one of two finders win the same bottle', async () => {
    await seedBottle(M1);
    const results = await Promise.allSettled([
      commit(guest('finn'), findWrites('finn', M1)),
      commit(guest('fern'), findWrites('fern', M1)),
    ]);
    assert.equal(results.filter((r) => r.status === 'fulfilled').length, 1);
  });

  // The accepted hole (owner decision, 2026-10-13): another plausible local date allows another find.
  it('lets a time-zone change buy one more find on another plausible date', async () => {
    await seedBottle(M1);
    await seedBottle(M2);
    const other = utcHour() < 12 ? utcDate(Date.now() - DAY) : utcDate(Date.now() + DAY);
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M1)));
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M2, { date: other, zone: 'Pacific/Kiritimati' })));
  });
});
