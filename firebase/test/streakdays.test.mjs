import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  collection, doc, getDoc, getDocs, query, setDoc, where,
} from 'firebase/firestore';
import {
  DAY, M1, M2, castWrites, commit, google, guest, seedProfile, seedTally, streakDayWrite, utcDate,
} from './support.mjs';

describe('streak days', () => {
  it('lets me mark a calm sea today, again, and read my days', async () => {
    const db = guest('finn');
    await assertSucceeds(commit(db, [streakDayWrite('finn', { calm: true })]));
    await assertSucceeds(commit(db, [streakDayWrite('finn', { calm: true })]));
    await assertSucceeds(getDoc(doc(db, 'streakDays', `finn_${utcDate()}`)));
    await assertSucceeds(getDocs(query(collection(db, 'streakDays'), where('uid', '==', 'finn'))));
  });

  it('lets me carry a streak over, and only ever raise it', async () => {
    const db = google('finn');
    await assertSucceeds(commit(db, [streakDayWrite('finn', { carried: 3 })]));
    await assertSucceeds(commit(db, [streakDayWrite('finn', { carried: 5 })]));
    await assertFails(commit(db, [streakDayWrite('finn', { carried: 2 })]));
  });

  for (const [label, marks] of [
    ['a calm mark that is false', { calm: false }],
    ['a carried streak of 0', { carried: 0 }],
    ['a carried streak that is not a whole number', { carried: 2.5 }],
    ['an extra key', { calm: true, shields: 2 }],
  ]) {
    it(`refuses ${label}`, async () => {
      await assertFails(commit(guest('finn'), [streakDayWrite('finn', marks)]));
    });
  }

  it('refuses another day than today, or someone else\'s day', async () => {
    const db = guest('finn');
    await assertFails(commit(db, [streakDayWrite('finn', { calm: true }, { date: utcDate(Date.now() - 3 * DAY) })]));
    await assertFails(commit(db, [streakDayWrite('fern', { calm: true })]));
    const date = utcDate();
    await assertFails(setDoc(doc(db, 'streakDays', `finn_${date}`), { uid: 'finn', date: '2026-01-01', calm: true }));
  });

  it('refuses dropping a mark', async () => {
    const db = guest('finn');
    const date = utcDate();
    await commit(db, [streakDayWrite('finn', { calm: true })]);
    await assertFails(setDoc(doc(db, 'streakDays', `finn_${date}`), { uid: 'finn', date }));
  });

  it("refuses reading someone else's days", async () => {
    await commit(guest('finn'), [streakDayWrite('finn', { calm: true })]);
    const db = guest('fern');
    await assertFails(getDoc(doc(db, 'streakDays', `finn_${utcDate()}`)));
    await assertFails(getDocs(query(collection(db, 'streakDays'), where('uid', '==', 'finn'))));
  });
});

describe('the cast mark', () => {
  it("marks the day's first cast in the cast's commit", async () => {
    const db = google('cass');
    await assertSucceeds(commit(db, [...castWrites('cass', M1), streakDayWrite('cass', { cast: true })]));
  });

  it('keeps a calm mark when the first cast comes later', async () => {
    const db = google('cass');
    await commit(db, [streakDayWrite('cass', { calm: true })]);
    await assertSucceeds(commit(db, [...castWrites('cass', M1), streakDayWrite('cass', { cast: true })]));
  });

  it('refuses a cast mark without a cast', async () => {
    await assertFails(commit(google('cass'), [streakDayWrite('cass', { cast: true })]));
  });

  it("refuses a cast mark with the day's second cast", async () => {
    await seedProfile('cass', { termsVersion: 1 });
    await seedTally('cass', { date: utcDate(), count: 1, messageId: M1 });
    const writes = castWrites('cass', M2, { tallyKind: 'update', profile: null });
    await assertSucceeds(commit(google('cass'), writes));
    await assertFails(commit(google('cass'), [streakDayWrite('cass', { cast: true })]));
  });
});
