import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';
import {
  DAY, cast, google, messageId, seedTally, utcDate,
} from './support.mjs';

// The nth cast of the day after the first: the same-day update, with the Terms already accepted.
function castAgain(db, uid, n, tally = {}) {
  return cast(db, uid, messageId(200 + n), {}, { tallyKind: 'update', profile: null, tally });
}

describe('five casts a day', () => {
  it('counts five casts, and refuses a sixth', async () => {
    const db = google('alice');
    await assertSucceeds(cast(db, 'alice', messageId(200)));
    for (let n = 1; n < 5; n += 1) {
      await assertSucceeds(castAgain(db, 'alice', n));
    }
    await assertFails(castAgain(db, 'alice', 5));
  });

  it('refuses skipping a count', async () => {
    const db = google('alice');
    await assertSucceeds(cast(db, 'alice', messageId(200)));
    await assertFails(cast(db, 'alice', messageId(201), {}, { profile: null, tally: { count: 3 } }));
  });

  it('refuses starting the same date again at 1', async () => {
    await seedTally('alice', { date: utcDate(), count: 3, messageId: messageId(1) });
    await assertFails(cast(google('alice'), 'alice', messageId(200)));
  });

  it('starts a newer date at 1, and only at 1', async () => {
    await seedTally('alice', { date: utcDate(Date.now() - 2 * DAY), count: 5, messageId: messageId(1) });
    await assertFails(cast(google('alice'), 'alice', messageId(200), {}, { tally: { count: 2 } }));
    await assertSucceeds(cast(google('alice'), 'alice', messageId(201)));
  });

  it('refuses going back a date', async () => {
    await seedTally('alice', { date: utcDate(Date.now() + DAY), count: 5, messageId: messageId(1) });
    await assertFails(cast(google('alice'), 'alice', messageId(200)));
  });

  for (const date of ['2026-02-30', 'yesterday', '2020-01-01', utcDate(Date.now() + 2 * DAY)]) {
    it(`refuses the date ${date}`, async () => {
      await assertFails(cast(google('alice'), 'alice', messageId(200), {}, { tally: { date } }));
    });
  }

  it('refuses an extra key', async () => {
    await assertFails(cast(google('alice'), 'alice', messageId(200), {}, { tally: { zone: 'UTC' } }));
  });

  it('refuses a tally on its own', async () => {
    await assertFails(setDoc(doc(google('alice'), 'castTally', 'alice'), {
      date: utcDate(), count: 1, messageId: messageId(200),
    }));
  });

  it('lets me read my tally, and nobody delete one', async () => {
    await seedTally('alice', { date: utcDate(), count: 1, messageId: messageId(1) });
    await assertSucceeds(getDoc(doc(google('alice'), 'castTally', 'alice')));
    await assertFails(getDoc(doc(google('bob'), 'castTally', 'alice')));
    await assertFails(deleteDoc(doc(google('alice'), 'castTally', 'alice')));
  });
});
