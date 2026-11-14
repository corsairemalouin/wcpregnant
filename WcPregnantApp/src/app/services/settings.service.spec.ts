import { SettingsService } from './settings.service';

describe('SettingsService', () => {
  const STORAGE_KEY = 'wc-pregnant-developer-mode';
  let service: SettingsService;

  beforeEach(() => {
    localStorage.removeItem(STORAGE_KEY);
    service = new SettingsService();
  });

  it('shouldCreate_withDeveloperModeDisabledByDefault', () => {
    expect(service).toBeTruthy();
    expect(service.isDeveloperMode).toBeFalse();
  });

  it('setDeveloperMode_shouldPersistPreference_whenEnabled', () => {
    service.setDeveloperMode(true);

    expect(service.isDeveloperMode).toBeTrue();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
  });

  it('setDeveloperMode_shouldClearPreference_whenDisabled', () => {
    service.setDeveloperMode(true);
    service.setDeveloperMode(false);

    expect(service.isDeveloperMode).toBeFalse();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('false');
  });

  it('toggleDeveloperMode_shouldFlipValue_whenCalled', () => {
    expect(service.toggleDeveloperMode()).toBeTrue();
    expect(service.toggleDeveloperMode()).toBeFalse();
  });

  it('shouldRestoreStoredMode_whenLocalStorageAlreadyContainsIt', () => {
    localStorage.setItem(STORAGE_KEY, 'true');

    const restored = new SettingsService();

    expect(restored.isDeveloperMode).toBeTrue();
  });

  it('shouldNotThrow_whenLocalStorageIsUnavailable', () => {
    const original = localStorage.setItem;
    localStorage.setItem = () => {
      throw new Error('storage unavailable');
    };

    try {
      service.setDeveloperMode(true);
      expect(service.isDeveloperMode).toBeTrue();
    } finally {
      localStorage.setItem = original;
    }
  });
});
