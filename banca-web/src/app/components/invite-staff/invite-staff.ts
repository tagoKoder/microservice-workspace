// src/app/components/invite-staff/invite-staff.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InviteStaffService } from './service/invite-staff-service';
import { InviteStaffResponse, InviteStaffRequest } from './service/invite-staff-model';
import { JsonPipe } from '@angular/common';

@Component({
  standalone: true,
  imports: [JsonPipe, FormsModule, RouterLink],
  selector: 'app-invite-staff',
  templateUrl: './invite-staff.html',
  styleUrls: ['./invite-staff.scss']
})
export class InviteStaffComponent {
  email = '';
  role: 'doctor' | 'lab' = 'doctor';
  loading = false;
  result?: InviteStaffResponse;

  constructor(private service: InviteStaffService) {}

  async submit() {
    this.loading = true;
    this.result = undefined;
    const payload: InviteStaffRequest = { email: this.email.trim(), role: this.role };

    this.service.inviteStaff(payload).subscribe({
      next: r => { this.result = r; this.loading = false; },
      error: e => { this.result = undefined; this.loading = false; }
    });
  }
}
