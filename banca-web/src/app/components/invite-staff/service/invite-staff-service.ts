import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { InviteStaffRequest, InviteStaffResponse } from './invite-staff-model';
import { environment } from '@env/environment';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class InviteStaffService {
     constructor(private http: HttpClient) {}

    /** POST /api/v1/administrator-web/identity/invitations (admin-only) */
  inviteStaff(payload: InviteStaffRequest): Observable<InviteStaffResponse> {
    return this.http.post<InviteStaffResponse>(`${environment.apiBase}/identity/invitations`, payload);
  }
}
