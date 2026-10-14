import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { Timestamp, addDoc, collection, doc, setDoc } from 'firebase/firestore';
import {
  cast, castFields, castWrites, commit, google, guest, linkedGuest, messageId, seedBottle, seedProfile,
  signedOut, withToken,
} from './support.mjs';

const NEW = messageId(100);

describe('casting a bottle', () => {
  it('lets a Google user cast the full shape', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW));
  });

  it('lets a guest that linked Google cast', async () => {
    await assertSucceeds(cast(linkedGuest('gina'), 'gina', NEW));
  });

  it('refuses a guest', async () => {
    await assertFails(cast(guest('gina'), 'gina', NEW));
  });

  it('refuses a token without identities', async () => {
    await assertFails(cast(withToken('tess', { firebase: { sign_in_provider: 'google.com' } }), 'tess', NEW));
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
    ['foundAt', Timestamp.now()], ['sealable', 'yes'], ['sealable', null],
    ['lang', 'English'], ['lang', 'EN'], ['lang', ''],
  ]) {
    it(`refuses ${field} = ${JSON.stringify(value)} on a new bottle`, async () => {
      await assertFails(cast(google('alice'), 'alice', NEW, { [field]: value }));
    });
  }

  it('accepts either seal choice', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { sealable: true }));
    await assertSucceeds(cast(google('bob'), 'bob', messageId(101), { sealable: false }));
  });

  it('accepts a language with a region', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { lang: 'es-419' }));
  });

  it('refuses a missing key', async () => {
    const writes = castWrites('alice', NEW);
    delete writes[0][2].lang;
    await assertFails(commit(google('alice'), writes));
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

  it('accepts 280 bytes of text, and refuses 282', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { messageContent: 'a'.repeat(280) }));
    await assertFails(cast(google('bob'), 'bob', messageId(102), { messageContent: '€'.repeat(94) }));
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

describe('the Terms, on a cast', () => {
  it('refuses a first cast that leaves the Terms unaccepted', async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, {}, { profile: null }));
  });

  it('lets a returning caster cast without accepting again', async () => {
    await seedProfile('alice', { termsVersion: 1, termsAcceptedAt: Timestamp.now() });
    await assertSucceeds(cast(google('alice'), 'alice', NEW, {}, { profile: null }));
  });

  it('refuses a profile with an older version', async () => {
    await seedProfile('alice', { termsVersion: 0, termsAcceptedAt: Timestamp.now() });
    await assertFails(cast(google('alice'), 'alice', NEW, {}, { profile: null }));
  });

  it("refuses accepting with the client's own time", async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, {}, { profile: { termsAcceptedAt: Timestamp.now() } }));
  });
});

describe('signing a bottle with a pen name', () => {
  it("accepts the profile's own pen name", async () => {
    await seedProfile('alice', { penName: 'Driftwood', termsVersion: 1, termsAcceptedAt: Timestamp.now() });
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { penName: 'Driftwood' }, { profile: null }));
  });

  it('accepts a pen name set in the same commit', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW, { penName: 'Quiet Otter' }, { profile: { penName: 'Quiet Otter' } }));
  });

  it("refuses a pen name that isn't the profile's", async () => {
    await seedProfile('alice', { penName: 'Driftwood', termsVersion: 1, termsAcceptedAt: Timestamp.now() });
    await assertFails(cast(google('alice'), 'alice', NEW, { penName: 'Tidewise' }, { profile: null }));
  });

  it('refuses a pen name when the profile has none', async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, { penName: 'Driftwood' }));
  });

  for (const penName of ['Al', 'A'.repeat(21), ' Driftwood', 'Driftwood ', 7]) {
    it(`refuses the pen name ${JSON.stringify(penName)}`, async () => {
      await assertFails(cast(google('alice'), 'alice', NEW, { penName }, { profile: { penName } }));
    });
  }
});

describe('the cast tally, on a cast', () => {
  it('refuses a cast without the tally', async () => {
    await assertFails(commit(google('alice'), castWrites('alice', NEW), [0, 2]));
  });

  it('refuses a tally naming another bottle', async () => {
    await assertFails(cast(google('alice'), 'alice', NEW, {}, { tally: { messageId: messageId(101) } }));
  });

  it('refuses two bottles counted once', async () => {
    const second = messageId(101);
    const writes = [...castWrites('alice', NEW), ['set', ['messages', second], castFields('alice', second)]];
    await assertFails(commit(google('alice'), writes));
  });

  it("refuses a tally that isn't mine", async () => {
    const writes = castWrites('alice', NEW);
    writes[1] = ['set', ['castTally', 'bob'], { ...writes[1][2] }];
    await assertFails(commit(google('alice'), writes));
  });

  it('refuses a tally that skips a count', async () => {
    await assertSucceeds(cast(google('alice'), 'alice', NEW));
    await assertFails(cast(google('alice'), 'alice', messageId(101), {}, { profile: null, tally: { count: 3 } }));
  });

  it('refuses touching only the profile and the tally', async () => {
    await assertFails(commit(google('alice'), castWrites('alice', NEW), [1, 2]));
  });
});
