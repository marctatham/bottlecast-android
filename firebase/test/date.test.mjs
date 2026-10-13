import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, serverTimestamp, Timestamp, updateDoc } from 'firebase/firestore';
import {
  DAY, M1, commit, findWrites, guest, seedBottle, seedHold, utcDate, utcHour,
} from './support.mjs';

// The daily record's date must be today somewhere on Earth: from UTC-12 to UTC+14.
describe('the daily record', () => {
  beforeEach(async () => {
    await seedBottle(M1);
  });

  it("accepts today's UTC date", async () => {
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M1, { date: utcDate() })));
  });

  it("accepts yesterday's UTC date while it is still yesterday at UTC-12", async function () {
    if (utcHour() >= 12) this.skip();
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M1, { date: utcDate(Date.now() - DAY) })));
  });

  it("accepts tomorrow's UTC date once it is tomorrow at UTC+14", async function () {
    if (utcHour() < 10) this.skip();
    await assertSucceeds(commit(guest('finn'), findWrites('finn', M1, { date: utcDate(Date.now() + DAY) })));
  });

  for (const days of [-2, 2]) {
    it(`refuses a date ${days} days away`, async () => {
      await assertFails(commit(guest('finn'), findWrites('finn', M1, { date: utcDate(Date.now() + days * DAY) })));
    });
  }

  for (const date of ['2026-1-05', '2026/10/13', `${utcDate()}x`, utcDate().replaceAll('-', ''), '', 'abcd-ef-gh', `${utcDate()}-01`]) {
    it(`refuses the malformed date ${JSON.stringify(date)}`, async () => {
      await assertFails(commit(guest('finn'), findWrites('finn', M1, { date })));
    });
  }

  it("refuses a day that doesn't exist, even if it would roll over to today", async () => {
    const now = new Date();
    const lastOfPreviousMonth = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 0)).getUTCDate();
    const year = now.getUTCMonth() === 0 ? now.getUTCFullYear() - 1 : now.getUTCFullYear();
    const month = now.getUTCMonth() === 0 ? 12 : now.getUTCMonth();
    const rolled = `${year}-${String(month).padStart(2, '0')}-${String(lastOfPreviousMonth + now.getUTCDate()).padStart(2, '0')}`;
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { date: rolled })));
  });

  for (const [label, zone] of [['an empty zone', ''], ['a 65-character zone', 'z'.repeat(65)], ['a numeric zone', 1]]) {
    it(`refuses ${label}`, async () => {
      await assertFails(commit(guest('finn'), findWrites('finn', M1, { zone })));
    });
  }

  it('refuses a daily record without a zone', async () => {
    const writes = findWrites('finn', M1);
    delete writes[2][2].zone;
    await assertFails(commit(guest('finn'), writes));
  });

  it("refuses the client's own foundAt on the daily record", async () => {
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { daily: { foundAt: Timestamp.now() } })));
  });

  it('refuses a uid that is not mine', async () => {
    await assertFails(commit(guest('finn'), findWrites('finn', M1, { daily: { uid: 'other' } })));
  });

  it('never lets a daily record change or go', async () => {
    await seedHold(M1, 'finn', 60 * 1000);
    const db = guest('finn');
    const ref = doc(db, 'daily', `finn_${utcDate(Date.now() - 60 * 1000)}`);
    await assertFails(updateDoc(ref, { zone: 'Asia/Tokyo' }));
    await assertFails(updateDoc(ref, { foundAt: serverTimestamp() }));
    await assertFails(deleteDoc(ref));
  });
});
