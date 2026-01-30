import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class IdempotencyKeyService {
  newKey(): string {
    // Navegadores modernos
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return (crypto as any).randomUUID();
    }
    // Fallback
    return `idem_${Date.now()}_${Math.random().toString(16).slice(2)}`;
  }
}
