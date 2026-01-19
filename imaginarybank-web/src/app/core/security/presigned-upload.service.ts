import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PresignedUploadService {
  constructor(private http: HttpClient) {}

  async putFile(url: string, file: File): Promise<{ etag?: string }> {
    const headers = new HttpHeaders({
      'Content-Type': file.type || 'application/octet-stream'
    });

    const res = await firstValueFrom(
      this.http.put(url, file, { headers, observe: 'response', responseType: 'text' })
    ) as HttpResponse<string>;

    // S3 responde ETag en header (a veces con comillas)
    const etag = res.headers.get('ETag')?.replaceAll('"', '') ?? undefined;
    return { etag };
  }
}
