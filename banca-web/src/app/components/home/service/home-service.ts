import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { WhoAmIResponse } from './home-service-model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HomeService {
   constructor(private http: HttpClient) {}
  /** GET /api/v1/administrator-web/identity/whoami (access_token via interceptor) */
  whoAmI(): Observable<WhoAmIResponse> {
    return this.http.get<WhoAmIResponse>(`${environment.apiBase}/identity/whoami`);
  }
}
