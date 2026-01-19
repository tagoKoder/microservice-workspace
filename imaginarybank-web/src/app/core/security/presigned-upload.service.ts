import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export type ExtraHeader = { name: string; value: string };

@Injectable({ providedIn: 'root' })
export class PresignedUploadService {
  constructor(private http: HttpClient) {}

  async putFile(url: string, file: File, extraHeaders: ExtraHeader[] = []): Promise<{ etag?: string }> {
    let headers = new HttpHeaders({
      'Content-Type': file.type || 'application/octet-stream'
    });

    for (const h of extraHeaders) {
      headers = headers.set(h.name, h.value);
    }

    const res = await firstValueFrom(
      this.http.put(url, file, { headers, observe: 'response', responseType: 'text' })
    ) as HttpResponse<string>;

    const etag = res.headers.get('ETag')?.replaceAll('"', '') ?? undefined;
    return { etag };
  }
}
