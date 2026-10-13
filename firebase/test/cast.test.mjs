import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { Timestamp, addDoc, collection, doc, setDoc } from 'firebase/firestore';
import {
  cast, castFields, google, guest, messageId, seedBottle, signedOut,
} from './support.mjs';

const NEW = messageId(100);

describe('casting a bottle', () => {
  it('lets a Google user cast the full shape', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW));
  });

  it('lets a guest cast, until slice 3 asks for Google', async () => {
    await assertSucceeds(cast(guest('gina'), 'gina', NEW));
  });

  it('refuses someone signed out', async () => {
    await assertFails(setDoc(doc(signedOut(), 'messages', NEW), castFields('nobody', NEW)));
  });

  it("refuses someone else's userId", async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, { userId: 'bob' }));
  });

  it("refuses the client's own createdAt", async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, { createdAt: Timestamp.now() }));
  });

  for (const [field, value] of [
    ['status', 'found'], ['status', 'corked'], ['reportCount', 1], ['findCount', 1],
    ['foundAt', Timestamp.now()], ['penName', 'Driftwood'], ['sealable', true],
    ['lang', 'English'], ['lang', 'EN'], ['lang', ''],
  ]) {
    it(`refuses ${field} = ${JSON.stringify(value)} on a new bottle`, async () => {
      await assertFails(cast(google('alice'), 'alice', NEW, { [field]: value }));
    });
  }

  it('accepts a language with a region', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { lang: 'es-419' }));
  });

  it('refuses a missing key', async () => {
    const fields = castFields('alice', NEW);
    delete fields.lang;
    await assertFails(setDoc(doc(google('alice'), 'messages', NEW), fields));
  });

  it('refuses an extra key', async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, { tint: 'amber' }));
  });

  it('refuses an id that differs from the document id', async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, { id: messageId(101) }));
  });

  for (const id of ['bottle_0000000000001', 'bottle0000000000001', 'bottle000000000000001']) {
    it(`refuses the document id ${id}`, async () => {
      await assertFails(cast(google('alice'), 'alice', id));
    });
  }

  it('refuses text under 10 characters once trimmed', async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, { messageContent: '   too short   ' }));
  });

  it('accepts 1000 bytes of text, and refuses 1002', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { messageContent: 'a'.repeat(1000) }));
    await assertFails(cast(google('alice'), 'alice', messageId(102), { messageContent: '€'.repeat(334) }));
  });

  for (const rand of [1, 1.5, -0.01, '0.5', 0]) {
    it(`refuses rand = ${JSON.stringify(rand)}`, async () => {
      await assertFails(cast(google('alice'), 'alice', NEW, { rand }));
    });
  }

  it("refuses 1.1.1's shape", async () => {
    const db = google('alice');
    await assertFails(addDoc(collection(db, 'messages'), { messageContent: 'An old-style bottle message', userId: 'alice' }));
  });

  it('refuses casting over an existing bottle', async () => {
    await seedBottle(NEW, { userId: 'alice' });
    await assertFails(cast(google('alice'), 'alice', NEW));
  });
});
