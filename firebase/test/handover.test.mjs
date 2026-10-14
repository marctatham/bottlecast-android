import assert from 'node:assert/strict';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  Bytes, Timestamp, collection, deleteDoc, doc, getDoc, getDocs, setDoc,
} from 'firebase/firestore';
import {
  DAY, HOUR, M1, WRITER, armHandoff, claimWrites, commit, google, guest, handoffFields, linkedGuest,
  newToken, reportWrites, seed, seedHold, throwBackWrites, tokenHash, utcDate,
} from './support.mjs';

// A guest, gina, holds M1 from an hour ago; her daily record carries that hour's date.
const HELD_FOR = HOUR;

function heldDate() {
  return utcDate(Date.now() - HELD_FOR);
}

async function armed(token = newToken()) {
  await seedHold(M1, 'gina', HELD_FOR);
  await assertSucceeds(armHandoff(guest('gina'), 'gina', M1, { date: heldDate(), hash: tokenHash(token) }));
  return token;
}

async function read(path) {
  let data;
  await seed(async (db) => { data = (await getDoc(doc(db, ...path))).data(); });
  return data;
}

describe('arming a handoff', () => {
  it("lets today's live holder arm one, and arm it again", async () => {
    await armed();
    await assertSucceeds(armHandoff(guest('gina'), 'gina', M1, { date: heldDate(), hash: tokenHash(newToken()) }));
  });

  it("refuses someone who doesn't hold the bottle", async () => {
    await seedHold(M1, 'gina', HELD_FOR);
    await assertFails(armHandoff(guest('fern'), 'fern', M1, { date: heldDate(), hash: tokenHash(newToken()) }));
  });

  for (const findStatus of ['thrown_back', 'reported']) {
    it(`refuses a find that was ${findStatus}`, async () => {
      await seedHold(M1, 'gina', HELD_FOR, { findStatus });
      await assertFails(armHandoff(guest('gina'), 'gina', M1, { date: heldDate(), hash: tokenHash(newToken()) }));
    });
  }

  it('refuses a hold that has run out', async () => {
    await seedHold(M1, 'gina', 8 * DAY);
    await assertFails(armHandoff(guest('gina'), 'gina', M1, {
      date: utcDate(Date.now() - 8 * DAY), hash: tokenHash(newToken()),
    }));
  });

  it("refuses a date with no daily record naming the bottle", async () => {
    await seedHold(M1, 'gina', HELD_FOR);
    await assertFails(armHandoff(guest('gina'), 'gina', M1, {
      date: utcDate(Date.now() + DAY), hash: tokenHash(newToken()),
    }));
  });

  it('refuses a Google account', async () => {
    await seedHold(M1, 'gina', HELD_FOR);
    await assertFails(armHandoff(linkedGuest('gina'), 'gina', M1, { date: heldDate(), hash: tokenHash(newToken()) }));
  });

  for (const [label, overrides] of [
    ["the client's createdAt", { createdAt: Timestamp.now() }],
    ['a short hash', { tokenHash: Bytes.fromUint8Array(new Uint8Array(31)) }],
    ['a hash as a string', { tokenHash: 'abc' }],
    ['a token already', { token: 'abc' }],
    ['a claim already', { claimedBy: 'alice' }],
    ['someone else as the giver', { from: 'fern' }],
  ]) {
    it(`refuses ${label}`, async () => {
      await seedHold(M1, 'gina', HELD_FOR);
      await assertFails(setDoc(doc(guest('gina'), 'handoffs', M1), {
        ...handoffFields('gina', { date: heldDate(), hash: tokenHash(newToken()) }), ...overrides,
      }));
    });
  }

  it('lets nobody read a handoff, or delete one', async () => {
    await armed();
    await assertFails(getDoc(doc(guest('gina'), 'handoffs', M1)));
    await assertFails(getDoc(doc(google('alice'), 'handoffs', M1)));
    await assertFails(getDocs(collection(guest('gina'), 'handoffs')));
    await assertFails(deleteDoc(doc(guest('gina'), 'handoffs', M1)));
  });
});

describe('claiming a handoff', () => {
  it('moves the live hold to the Google account that presents the token', async () => {
    const token = await armed();
    await assertSucceeds(commit(google('alice'), claimWrites('alice', M1, token)));

    const message = await read(['messages', M1]);
    assert.equal(message.status, 'found');
    assert.equal(message.findCount, 1);

    // the guest's find no longer holds it, and the new holder's does
    await assertFails(commit(guest('gina'), throwBackWrites('gina', M1)));
    await assertFails(commit(guest('gina'), reportWrites('gina', M1)));
    await assertSucceeds(commit(google('alice'), throwBackWrites('alice', M1)));
  });

  it('refuses the wrong token', async () => {
    await armed();
    await assertFails(commit(google('alice'), claimWrites('alice', M1, newToken())));
  });

  it('refuses a handoff older than 10 minutes', async () => {
    const token = newToken();
    await seedHold(M1, 'gina', HELD_FOR);
    await seed((db) => setDoc(doc(db, 'handoffs', M1), {
      ...handoffFields('gina', { date: heldDate(), hash: tokenHash(token) }),
      createdAt: Timestamp.fromMillis(Date.now() - 11 * 60 * 1000),
    }));
    await assertFails(commit(google('alice'), claimWrites('alice', M1, token)));
  });

  it('refuses a second claim', async () => {
    const token = await armed();
    await assertSucceeds(commit(google('alice'), claimWrites('alice', M1, token)));
    await assertFails(commit(google('bob'), claimWrites('bob', M1, token)));
  });

  it('refuses a guest, or the giver itself', async () => {
    const token = await armed();
    await assertFails(commit(guest('fern'), claimWrites('fern', M1, token)));
    await assertFails(commit(linkedGuest('gina'), claimWrites('gina', M1, token)));
  });

  it('refuses a Google account that already has a find today', async () => {
    const token = await armed();
    await seed((db) => setDoc(doc(db, 'daily', `alice_${utcDate()}`), {
      uid: 'alice', date: utcDate(), zone: 'UTC', messageId: 'another', foundAt: Timestamp.now(),
    }));
    await assertFails(commit(google('alice'), claimWrites('alice', M1, token)));
  });

  it('refuses a Google account that found this bottle before', async () => {
    const token = await armed();
    await seed((db) => setDoc(doc(db, 'finds', `${M1}_alice`), {
      messageId: M1, finderUid: 'alice', foundAt: Timestamp.fromMillis(Date.now() - 30 * DAY),
      dailyId: 'alice_2026-01-01', status: 'thrown_back',
    }));
    await assertFails(commit(google('alice'), claimWrites('alice', M1, token)));
  });

  it('refuses the bottle going to its own writer', async () => {
    const token = await armed();
    await assertFails(commit(google(WRITER), claimWrites(WRITER, M1, token)));
  });

  it('refuses a hold the guest threw back after arming', async () => {
    const token = await armed();
    await assertSucceeds(commit(guest('gina'), throwBackWrites('gina', M1)));
    await assertFails(commit(google('alice'), claimWrites('alice', M1, token)));
  });

  for (const [label, message] of [
    ['a status change', { status: 'afloat' }], ['a find count', { findCount: 2 }], ['a report count', { reportCount: 1 }],
  ]) {
    it(`refuses the claim carrying ${label}`, async () => {
      const token = await armed();
      await assertFails(commit(google('alice'), claimWrites('alice', M1, token, { message })));
    });
  }

  for (const only of [[0], [1], [2], [3], [0, 1], [0, 1, 2], [0, 1, 3], [1, 2, 3], [0, 2, 3]]) {
    it(`refuses part of the claim on its own (${only})`, async () => {
      const token = await armed();
      await assertFails(commit(google('alice'), claimWrites('alice', M1, token), only));
    });
  }
});
