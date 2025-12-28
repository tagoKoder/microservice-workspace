import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '@env/environment';

@Injectable({
  providedIn: 'root'
})
export class CallbackService {
  constructor(private http: HttpClient) {}
  link(idToken: string) {
    const headers = new HttpHeaders().set('X-ID-Token', idToken);
    return this.http.post(`${environment.apiBase}/identity/link`, {}, { headers });
  }
}
