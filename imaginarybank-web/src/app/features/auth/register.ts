import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CommonModule } from '@angular/common';

import { CardModule } from 'primeng/card';
import { StepsModule } from 'primeng/steps';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CheckboxModule } from 'primeng/checkbox';
import { DividerModule } from 'primeng/divider';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';

import { FileUploadModule } from 'primeng/fileupload';
import { InputNumberModule } from 'primeng/inputnumber';

import { MenuItem } from 'primeng/api';
import { Router } from '@angular/router';

import { OnboardingApi } from '../../api/bff';
import { PresignedUploadService } from '../../core/security/presigned-upload.service';
import { IdempotencyKeyService } from '../../core/security/idempotency.service';

type OccupationOption = { label: string; value: string };

@Component({
  standalone: true,
  selector: 'app-register-page',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    StepsModule,
    ButtonModule,
    InputTextModule,
    CheckboxModule,
    DividerModule,
    SelectModule,
    DatePickerModule,
    FileUploadModule,
    InputNumberModule
  ],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class Register {
  steps: MenuItem[] = [
    { label: 'Contacto + KYC' },
    { label: 'Activación' }
  ];
  activeIndex = 0;
  busy = false;

  registrationId: string | null = null;

  idFile: File | null = null;
  selfieFile: File | null = null;

  // Presigned uploads retornados por /intents
  private idFrontUpload: any | null = null;
  private selfieUpload: any | null = null;

  occupationOptions: OccupationOption[] = [
    { label: 'Estudiante', value: 'student' },
    { label: 'Empleado', value: 'employee'},
    { label: 'Independiente', value: 'self_employed' },
    { label: 'Desempleado', value: 'unemployed' },
    { label: 'Jubilado', value: 'retired' }
  ];

  contactForm!: FormGroup;
  activateForm!: FormGroup;

  private intentKey: string | null = null;
  private confirmKycKey: string | null = null;
  private activateKey: string | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private onboardingApi: OnboardingApi,
    private presigned: PresignedUploadService,
    private idempotency: IdempotencyKeyService
  ) {
    this.contactForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.minLength(7)]],

      national_id: ['', [Validators.required, Validators.minLength(5)]],
      national_id_issue_date: ['', [Validators.required]],

      fingerprint_code: ['', [Validators.required, Validators.minLength(4)]],
      monthly_income: [null, [Validators.required]],
      occupation_type: ['employee', [Validators.required]]
    });

    this.activateForm = this.fb.group({
      full_name: ['', [Validators.required, Validators.minLength(3)]],
      tin: ['', [Validators.required, Validators.minLength(5)]],
      birth_date: ['', [Validators.required]],
      country: ['EC', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      accepted: [false, [Validators.requiredTrue]]
    });
  }

  goLogin(): void {
    this.router.navigateByUrl('/login');
  }

  back(): void {
    this.activeIndex = Math.max(0, this.activeIndex - 1);
  }

  onIdSelected(ev: any): void {
    const file = ev?.files?.[0] || null;
    this.idFile = file;
  }

  onSelfieSelected(ev: any): void {
    const file = ev?.files?.[0] || null;
    this.selfieFile = file;
  }

  private pickUpload(intent: any, docType: 'id_front' | 'selfie'): any | null {
    const uploads = intent?.uploads || [];
    return uploads.find((u: any) => u.doc_type === docType) ?? null;
  }

  private validateFileAgainstPresigned(file: File, upload: any): void {
    // max_bytes
    const maxBytes = upload?.max_bytes;
    if (typeof maxBytes === 'number' && file.size > maxBytes) {
      throw new Error(`El archivo excede el tamaño máximo permitido (${maxBytes} bytes).`);
    }

    // content_type (si viene)
    const expectedCt = upload?.content_type;
    if (expectedCt && file.type && file.type !== expectedCt) {
      throw new Error(`Content-Type inválido. Esperado: ${expectedCt}. Recibido: ${file.type}.`);
    }
  }

  async startIntent(): Promise<void> {
    if (!this.idFile || !this.selfieFile) return;

    this.busy = true;
    try {
      const v = this.contactForm.value;
      this.intentKey = this.intentKey ?? this.idempotency.newKey();
      // 1) Intent (JSON)
      const intent = await firstValueFrom(
        this.onboardingApi.startOnboarding({
          idempotencyKey: this.intentKey,
          onboardingIntentRequestDto: {
            email: v.email,
            phone: v.phone,
            channel: 'web',
            locale: 'es-EC',

            national_id: v.national_id,
            national_id_issue_date: this.toDateString(v.national_id_issue_date),
            fingerprint_code: v.fingerprint_code,

            monthly_income: v.monthly_income,
            occupation_type: v.occupation_type,

            id_front_content_type: this.idFile.type || 'application/octet-stream',
            selfie_content_type: this.selfieFile.type || 'application/octet-stream'
          }
        })
      );

      this.registrationId = (intent as any).registration_id;

      this.idFrontUpload = this.pickUpload(intent, 'id_front');
      this.selfieUpload = this.pickUpload(intent, 'selfie');

      if (!this.registrationId || !this.idFrontUpload?.upload_url || !this.selfieUpload?.upload_url) {
        throw new Error('Respuesta inválida: faltan presigned uploads (id_front/selfie).');
      }

      // 2) Validar archivos contra presigned (size/content-type)
      this.validateFileAgainstPresigned(this.idFile, this.idFrontUpload);
      this.validateFileAgainstPresigned(this.selfieFile, this.selfieUpload);

      // 3) Subir a S3 directo con headers presigned
      const [idRes, selfieRes] = await Promise.all([
        this.presigned.putFile(this.idFrontUpload.upload_url, this.idFile, this.idFrontUpload.headers || []),
        this.presigned.putFile(this.selfieUpload.upload_url, this.selfieFile, this.selfieUpload.headers || [])
      ]);
      this.confirmKycKey = this.confirmKycKey ?? this.idempotency.newKey();
      // 4) Confirmar KYC en backend
      await firstValueFrom(
        this.onboardingApi.confirmOnboardingKyc({
          idempotencyKey: this.confirmKycKey,
          confirmKycRequestDto: {
            registration_id: this.registrationId,
            channel: 'web',
            objects: [
              {
                doc_type: 'id_front',
                bucket: this.idFrontUpload.bucket,
                key: this.idFrontUpload.key,
                etag: idRes.etag,
                size_bytes: this.idFile.size,
                content_type: this.idFile.type
              },
              {
                doc_type: 'selfie',
                bucket: this.selfieUpload.bucket,
                key: this.selfieUpload.key,
                etag: selfieRes.etag,
                size_bytes: this.selfieFile.size,
                content_type: this.selfieFile.type
              }
            ]
          }
        })
      );

      // 5) Ir a activación
      this.activeIndex = 1;

    } finally {
      this.busy = false;
    }
  }

  async activate(): Promise<void> {
    if (!this.registrationId) return;

    this.busy = true;
    try {
      const v1 = this.contactForm.value;
      const v2 = this.activateForm.value;

      this.activateKey = this.activateKey ?? this.idempotency.newKey();
      await firstValueFrom(
        this.onboardingApi.activateOnboarding({
          idempotencyKey: this.activateKey,
          activateRequestDto: {
            registration_id: this.registrationId,
            full_name: v2.full_name,
            tin: v2.tin,
            birth_date: this.toDateString(v2.birth_date),
            country: v2.country,
            email: v1.email,
            phone: v1.phone,
            accepted_terms: !!v2.accepted
          }
        })
      );

      // Activado -> ir a login (Cognito)
      await this.router.navigateByUrl('/home');
    } finally {
      this.busy = false;
    }
  }

  private toDateString(v: any): string {
    // p-calendar puede entregar Date
    if (v instanceof Date) {
      const y = v.getFullYear();
      const m = String(v.getMonth() + 1).padStart(2, '0');
      const d = String(v.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }
    return String(v);
  }
}
