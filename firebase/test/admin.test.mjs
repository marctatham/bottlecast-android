import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  Timestamp, collection, deleteDoc, doc, getDocs, orderBy, query, updateDoc, where,
} from 'firebase/firestore';
import {
  DAY, HOUR, M1, M2, admin, google, seedBottle, seedHold, withToken,
} from './support.mjs';

describe('moderation', () => {
  it('lets an admin pull a bottle, remove it, or restore it', async () => {
    await seedBottle(M1, { reportCount: 3 });
    const ref = doc(admin(), 'messages', M1);
    await assertSucceeds(updateDoc(ref, { status: 'under_review' }));
    await assertSucceeds(updateDoc(ref, { status: 'removed' }));
    await assertSucceeds(updateDoc(ref, { status: 'afloat', foundAt: null, reportCount: 0 }));
  });

  it('lets an admin pull a bottle someone holds', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertSucceeds(updateDoc(doc(admin(), 'messages', M1), { status: 'under_review' }));
  });

  it('lets an admin delete a bottle and list the reports', async () => {
    await seedBottle(M1);
    const db = admin();
    await assertSucceeds(getDocs(query(collection(db, 'reports'), orderBy('createdAt', 'desc'))));
    await assertSucceeds(getDocs(query(collection(db, 'messages'), where('status', '==', 'under_review'))));
    await assertSucceeds(deleteDoc(doc(db, 'messages', M1)));
  });

  it('refuses putting a bottle back in the sea while a finder holds it', async () => {
    await seedHold(M1, 'finn', HOUR, { reportCount: 1 });
    const ref = doc(admin(), 'messages', M1);
    await assertFails(updateDoc(ref, { status: 'afloat' }));
    await assertFails(updateDoc(ref, { status: 'afloat', foundAt: null, reportCount: 0 }));
  });

  it('restores a held bottle to its finder, with its reports cleared', async () => {
    await seedHold(M1, 'finn', HOUR, { reportCount: 1 });
    await assertSucceeds(updateDoc(doc(admin(), 'messages', M1), { status: 'found', reportCount: 0 }));
  });

  it('restores a pulled bottle to the finder whose hold still runs, and to the sea once it has ended', async () => {
    const ref = doc(admin(), 'messages', M1);
    await seedHold(M1, 'finn', HOUR, { reportCount: 1 });
    await assertSucceeds(updateDoc(ref, { status: 'under_review' }));
    await assertFails(updateDoc(ref, { status: 'afloat', foundAt: null, reportCount: 0 }));
    await assertSucceeds(updateDoc(ref, { status: 'found', reportCount: 0 }));

    await seedHold(M2, 'finn', 8 * DAY, { reportCount: 1 });
    const ended = doc(admin(), 'messages', M2);
    await assertSucceeds(updateDoc(ended, { status: 'under_review' }));
    await assertFails(updateDoc(ended, { status: 'found', reportCount: 0 }));
    await assertSucceeds(updateDoc(ended, { status: 'afloat', foundAt: null, reportCount: 0 }));
  });

  it('refuses changing a hold time while pulling or removing a bottle', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertFails(updateDoc(doc(admin(), 'messages', M1), { status: 'under_review', foundAt: Timestamp.now() }));
    await assertFails(updateDoc(doc(admin(), 'messages', M1), { status: 'removed', foundAt: null }));
  });

  it('refuses a status that moderation does not use', async () => {
    await seedBottle(M1);
    await assertFails(updateDoc(doc(admin(), 'messages', M1), { status: 'kept' }));
    await assertFails(updateDoc(doc(admin(), 'messages', M1), { status: 'found', foundAt: Timestamp.now() }));
  });

  it('refuses moderation changing the text', async () => {
    await seedBottle(M1);
    await assertFails(updateDoc(doc(admin(), 'messages', M1), { messageContent: 'Edited by a moderator.' }));
  });

  it("refuses a claim that isn't the boolean true", async () => {
    await seedBottle(M1);
    for (const claim of ['true', false, 1]) {
      const db = withToken('moderator', { admin: claim });
      await assertFails(updateDoc(doc(db, 'messages', M1), { status: 'removed' }));
      await assertFails(getDocs(collection(db, 'reports')));
    }
  });

  it('refuses everyone else, Google account or not', async () => {
    await seedBottle(M1);
    const db = google('alice');
    await assertFails(updateDoc(doc(db, 'messages', M1), { status: 'removed' }));
    await assertFails(deleteDoc(doc(db, 'messages', M1)));
  });
});
