import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  Timestamp, deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc,
} from 'firebase/firestore';
import { google, guest, seedProfile } from './support.mjs';

function profile(db, uid) {
  return doc(db, 'users', uid);
}

describe('my profile', () => {
  it('lets a Google account set, change and clear its pen name', async () => {
    const db = google('alice');
    await assertSucceeds(setDoc(profile(db, 'alice'), { penName: 'Driftwood' }, { merge: true }));
    await assertSucceeds(setDoc(profile(db, 'alice'), { penName: 'Quiet Otter' }, { merge: true }));
    await assertSucceeds(updateDoc(profile(db, 'alice'), { penName: null }));
  });

  it('refuses a guest', async () => {
    await assertFails(setDoc(profile(guest('gina'), 'gina'), { penName: 'Driftwood' }));
  });

  it('lets me read my profile, and only mine', async () => {
    await seedProfile('alice', { penName: 'Driftwood' });
    await assertSucceeds(getDoc(profile(google('alice'), 'alice')));
    await assertFails(getDoc(profile(google('bob'), 'alice')));
    await assertFails(setDoc(profile(google('bob'), 'alice'), { penName: 'Tidewise' }));
  });

  for (const penName of ['Al', 'A'.repeat(21), ' Driftwood', 'Driftwood ', 42]) {
    it(`refuses the pen name ${JSON.stringify(penName)}`, async () => {
      await assertFails(setDoc(profile(google('alice'), 'alice'), { penName }));
    });
  }

  it('refuses an extra key', async () => {
    await assertFails(setDoc(profile(google('alice'), 'alice'), { penName: 'Driftwood', name: 'Alice' }));
  });

  it('never lets anyone delete a profile', async () => {
    await seedProfile('alice', { penName: 'Driftwood' });
    await assertFails(deleteDoc(profile(google('alice'), 'alice')));
  });
});

describe('accepting the Terms', () => {
  it("accepts with the server's time", async () => {
    await assertSucceeds(setDoc(profile(google('alice'), 'alice'), {
      termsVersion: 1, termsAcceptedAt: serverTimestamp(),
    }, { merge: true }));
  });

  it("refuses the client's time", async () => {
    await assertFails(setDoc(profile(google('alice'), 'alice'), { termsVersion: 1, termsAcceptedAt: Timestamp.now() }));
  });

  it('refuses a version without its time, or a time without its version', async () => {
    await assertFails(setDoc(profile(google('alice'), 'alice'), { termsVersion: 1 }));
    await assertFails(setDoc(profile(google('alice'), 'alice'), { termsAcceptedAt: serverTimestamp() }));
  });

  for (const termsVersion of [0, '1', 1.5]) {
    it(`refuses the version ${JSON.stringify(termsVersion)}`, async () => {
      await assertFails(setDoc(profile(google('alice'), 'alice'), { termsVersion, termsAcceptedAt: serverTimestamp() }));
    });
  }

  it('accepts the same version again, as an offline phone might', async () => {
    await seedProfile('alice', { termsVersion: 1, termsAcceptedAt: Timestamp.fromMillis(Date.now() - 60_000) });
    await assertSucceeds(setDoc(profile(google('alice'), 'alice'), {
      termsVersion: 1, termsAcceptedAt: serverTimestamp(),
    }, { merge: true }));
  });

  it('refuses going back a version', async () => {
    await seedProfile('alice', { termsVersion: 2, termsAcceptedAt: Timestamp.now() });
    await assertFails(setDoc(profile(google('alice'), 'alice'), {
      termsVersion: 1, termsAcceptedAt: serverTimestamp(),
    }, { merge: true }));
  });

  it('refuses changing when I accepted without accepting again', async () => {
    await seedProfile('alice', { termsVersion: 1, termsAcceptedAt: Timestamp.fromMillis(Date.now() - 60_000) });
    await assertFails(updateDoc(profile(google('alice'), 'alice'), { termsAcceptedAt: Timestamp.now() }));
  });

  it('keeps the Terms while the pen name changes', async () => {
    await seedProfile('alice', { termsVersion: 1, termsAcceptedAt: Timestamp.now() });
    await assertSucceeds(setDoc(profile(google('alice'), 'alice'), { penName: 'Driftwood' }, { merge: true }));
  });
});
