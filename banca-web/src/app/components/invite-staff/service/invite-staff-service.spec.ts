import { TestBed } from '@angular/core/testing';

import { InviteStaffService } from './invite-staff-service';

describe('InviteStaffService', () => {
  let service: InviteStaffService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(InviteStaffService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
