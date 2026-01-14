// src/app/components/accept-invite/accept-invite.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { JsonPipe } from '@angular/common';
import { AcceptInvitationService } from './service/accept-invitation-service';

@Component({
  standalone: true,
  imports: [RouterLink, JsonPipe],
  templateUrl: './accept-invitation.html',
  styleUrls: ['./accept-invitation.scss']
})
export class AcceptInvitationComponent implements OnInit {
  state: 'loading' | 'ok' | 'error' = 'loading';
  detail: any;

  constructor(private route: ActivatedRoute, private service: AcceptInvitationService) {}

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!token) {
      this.state = 'error';
      this.detail = 'Falta token de invitación.';
      return;
    }
    this.service.acceptInvite(token).subscribe({
      next: r => { this.state = 'ok'; this.detail = r; },
      error: e => { this.state = 'error'; this.detail = e?.error ?? e?.message ?? e; }
    });
  }
}
