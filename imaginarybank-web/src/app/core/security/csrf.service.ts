import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, finalize, map, of, shareReplay, tap } from 'rxjs';
import { SessionApi } from '../../api/bff';

@Injectable({ providedIn: 'root' })
export class CsrfService {
  private token$ = new BehaviorSubject<string | null>(null);
  private inflight?: Observable<string>;

  constructor(private sessionApi: SessionApi) {}

  getToken(): Observable<string> {
    const current = this.token$.value;
    if (current) return of(current);

    if (!this.inflight) {
      this.inflight = this.sessionApi.getWebCsrfToken().pipe(
        map(r => r.csrf_token),
        tap(t => this.token$.next(t)),
        shareReplay(1),
        finalize(() => (this.inflight = undefined))
      );
    }
    return this.inflight;
  }

  clear(): void {
    this.token$.next(null);
  }
}
