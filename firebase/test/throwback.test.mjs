import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { Timestamp, deleteDoc, doc, setDoc, updateDoc } from 'firebase/firestore';
import {
  DAY, HOUR, M1, commit, guest, seed, seedHold, throwBackWrites, utcDate,
} from './support.mjs';

describe('throwing a bottle back', () => {
  it('lets the holder release it, together with their find', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertSucceeds(commit(guest('finn'), throwBackWrites('finn', M1)));
  });

  it('lets the holder mark just their find once the hold has expired', async () => {
    await seedHold(M1, 'finn', 8 * DAY);
    await assertSucceeds(commit(guest('finn'), throwBackWrites('finn', M1), [0]));
  });

  it('refuses releasing the bottle without the find', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertFails(commit(guest('finn'), throwBackWrites('finn', M1), [1]));
  });

  it("refuses someone who doesn't hold it", async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertFails(commit(guest('fern'), throwBackWrites('fern', M1)));
  });

  it('refuses an old finder once someone else holds it', async () => {
    const old = Timestamp.fromMillis(Date.now() - 9 * DAY);
    await seedHold(M1, 'second', HOUR, { findCount: 2 });
    await seed((db) => setDoc(doc(db, 'finds', `${M1}_first`), {
      messageId: M1, finderUid: 'first', foundAt: old, dailyId: `first_${utcDate(Date.now() - 9 * DAY)}`, status: 'held',
    }));
    await assertFails(commit(guest('first'), throwBackWrites('first', M1)));
  });

  it('refuses releasing once the hold has expired', async () => {
    await seedHold(M1, 'finn', 8 * DAY);
    await assertFails(commit(guest('finn'), throwBackWrites('finn', M1)));
  });

  for (const [label, message] of [
    ['a changed find count', { status: 'afloat', foundAt: null, findCount: 0 }],
    ['a changed rand', { status: 'afloat', foundAt: null, rand: 0.1 }],
    ['a foundAt left set', { status: 'afloat', foundAt: Timestamp.now() }],
  ]) {
    it(`refuses ${label}`, async () => {
      await seedHold(M1, 'finn', HOUR);
      const writes = throwBackWrites('finn', M1);
      writes[1][2] = message;
      await assertFails(commit(guest('finn'), writes));
    });
  }

  it('refuses a reporter throwing the bottle back', async () => {
    await seedHold(M1, 'finn', HOUR, { findStatus: 'reported', reportCount: 1 });
    await assertFails(commit(guest('finn'), throwBackWrites('finn', M1)));
    await assertFails(commit(guest('finn'), throwBackWrites('finn', M1), [0]));
  });

  it('refuses a find going back to held', async () => {
    await seedHold(M1, 'finn', HOUR, { findStatus: 'thrown_back' });
    await assertFails(updateDoc(doc(guest('finn'), 'finds', `${M1}_finn`), { status: 'held' }));
  });

  it("refuses changing a find's fixed fields, or deleting it", async () => {
    await seedHold(M1, 'finn', HOUR);
    const ref = doc(guest('finn'), 'finds', `${M1}_finn`);
    await assertFails(updateDoc(ref, { status: 'thrown_back', messageId: 'bottle00000000000099' }));
    await assertFails(updateDoc(ref, { foundAt: Timestamp.now() }));
    await assertFails(deleteDoc(ref));
  });
});
