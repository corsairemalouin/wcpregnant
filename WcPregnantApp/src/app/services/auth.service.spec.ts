import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('login_shouldBuildBasicHeader_whenCredentialsProvided', () => {
    service.login('manu', 'manu');

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.header).toBe(`Basic ${btoa('manu:manu')}`);
  });

  it('logout_shouldClearHeader_whenCalled', () => {
    service.login('admin', 'admin');
    service.logout();

    expect(service.header).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('header_shouldBeNull_whenNeverLoggedIn', () => {
    expect(service.header).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });
});
