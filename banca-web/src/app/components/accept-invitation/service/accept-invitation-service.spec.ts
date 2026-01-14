import { TestBed } from '@angular/core/testing';

import { AcceptInvitationService } from './accept-invitation-service';

describe('AcceptInvitationService', () => {
  let service: AcceptInvitationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AcceptInvitationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
