import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Reutiliza el tipo generado si lo tienes disponible.
// Si no, define la interfaz mínima aquí.
export interface OnboardingIntentResponse {
  registration_id: string;
  otp_channel_hint: string;
}

@Injectable({ providedIn: 'root' })
export class OnboardingUploadApi {
  constructor(private http: HttpClient) {}

  startOnboarding(form: FormData): Observable<OnboardingIntentResponse> {
    // Asumiendo mismo host del BFF / reverse proxy
    return this.http.post<OnboardingIntentResponse>('/api/v1/onboarding/intents', form);
  }
}
