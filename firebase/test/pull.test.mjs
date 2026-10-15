import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc } from 'firebase/firestore';
import {
  DAY, HOUR, M1, WRITER, google, seedBottle, seedHold, seedKept,
} from './support.mjs';

describe('pulling a bottle from the sea', () => {
  it('lets the writer pull a drifting bottle', async () => {
    await seedBottle(M1);
    await assertSucceeds(deleteDoc(doc(google(WRITER), 'messages', M1)));
  });

  it('lets the writer pull a bottle whose hold has ended', async () => {
    await seedHold(M1, 'finn', 8 * DAY);
    await assertSucceeds(deleteDoc(doc(google(WRITER), 'messages', M1)));
  });

  it('refuses pulling a bottle someone holds or kept', async () => {
    await seedHold(M1, 'finn', HOUR);
    await assertFails(deleteDoc(doc(google(WRITER), 'messages', M1)));
    await seedKept('bottle00000000000002', 'finn', 8 * DAY);
    await assertFails(deleteDoc(doc(google(WRITER), 'messages', 'bottle00000000000002')));
  });

  it('refuses pulling a bottle out for review', async () => {
    await seedBottle(M1, { reportCount: 3 });
    await assertFails(deleteDoc(doc(google(WRITER), 'messages', M1)));
    await seedBottle('bottle00000000000002', { status: 'under_review' });
    await assertFails(deleteDoc(doc(google(WRITER), 'messages', 'bottle00000000000002')));
  });

  it("refuses pulling someone else's bottle", async () => {
    await seedBottle(M1);
    await assertFails(deleteDoc(doc(google('fern'), 'messages', M1)));
  });
});
