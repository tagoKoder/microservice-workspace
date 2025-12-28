import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InviteStaff } from './invite-staff';

describe('InviteStaff', () => {
  let component: InviteStaff;
  let fixture: ComponentFixture<InviteStaff>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InviteStaff]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InviteStaff);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
