import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AcceptInvitation } from './accept-invitation';

describe('AcceptInvitation', () => {
  let component: AcceptInvitation;
  let fixture: ComponentFixture<AcceptInvitation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AcceptInvitation]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AcceptInvitation);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
