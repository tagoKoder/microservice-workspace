import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '@env/environment';
@Injectable({
  providedIn: 'root'
})
export class AcceptInvitationService {
  constructor(private http: HttpClient) {}
  /** POST /api/v1/public/identity/invitations/accept?token=... (pública) */
  acceptInvite(token: string) {
    const params = new HttpParams().set('token', token);
    return this.http.post(`${environment.apiBase}/identity/invitations/accept`, {}, { params });
  }
}
