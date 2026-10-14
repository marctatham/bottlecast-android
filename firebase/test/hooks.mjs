// Mocha root hooks: one test environment for the run, and an empty database for every test.
import { clearData, setUpEnvironment, tearDownEnvironment } from './support.mjs';

export const mochaHooks = {
  async beforeAll() {
    await setUpEnvironment();
  },
  async afterAll() {
    await tearDownEnvironment();
  },
  async beforeEach() {
    await clearData();
  },
};
