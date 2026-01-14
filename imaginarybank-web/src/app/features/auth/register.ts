import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { CardModule } from 'primeng/card';
import { StepsModule } from 'primeng/steps';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CheckboxModule } from 'primeng/checkbox';
import { DividerModule } from 'primeng/divider';

import { MenuItem } from 'primeng/api';
import { Router } from '@angular/router';

import { OnboardingApi } from '../../api/bff'; // para verify/consents/activate como ya lo usas

@Component({
  standalone: true,
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    CardModule,
    StepsModule,
    ButtonModule,
    InputTextModule,
    CheckboxModule,
    DividerModule
  ],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class Register {
  steps: MenuItem[] = [
    { label: 'Contacto + KYC' },
    { label: 'OTP' },
    { label: 'Consentimientos' },
    { label: 'Activación' }
  ];
  activeIndex = 0;
  busy = false;

  registrationId: string | null = null;
  otpChannelHint: string | null = null;

  idFile: File | null = null;
  selfieFile: File | null = null;

  contactForm!: FormGroup;
  otpForm!: FormGroup;
  consentForm!: FormGroup;
  activateForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private onboardingApi: OnboardingApi,
  ) {
    this.contactForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.minLength(7)]],

      national_id: ['', [Validators.required, Validators.minLength(5)]],
      national_id_issue_date: ['', [Validators.required]], // YYYY-MM-DD
      fingerprint_code: ['', [Validators.required, Validators.minLength(4)]],
      monthly_income: [null, [Validators.required]],
      occupation_type: ['employee', [Validators.required]]
    });

    this.otpForm = this.fb.group({ otp: ['', [Validators.required, Validators.minLength(4)]] });
    this.consentForm = this.fb.group({ accepted: [false, [Validators.requiredTrue]] });

    this.activateForm = this.fb.group({
      full_name: ['', [Validators.required, Validators.minLength(3)]],
      tin: ['', [Validators.required, Validators.minLength(5)]],
      birth_date: ['', [Validators.required]],
      country: ['EC', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]]
    });
  }

  goLogin(): void {
    this.router.navigateByUrl('/login');
  }

  back(): void {
    this.activeIndex = Math.max(0, this.activeIndex - 1);
  }

  onIdFile(ev: Event): void {
    const file = (ev.target as HTMLInputElement).files?.[0] || null;
    this.idFile = file;
  }

  onSelfieFile(ev: Event): void {
    const file = (ev.target as HTMLInputElement).files?.[0] || null;
    this.selfieFile = file;
  }

  async startIntent(): Promise<void> {
    if (!this.idFile || !this.selfieFile) return;

    this.busy = true;
    try {
      const v = this.contactForm.value;

      const r = await firstValueFrom(
        this.onboardingApi.startOnboarding({
          email: v.email,
          phone: v.phone,
          channel: 'web',
          locale: 'es-EC',

          nationalId: v.national_id,
          nationalIdIssueDate: v.national_id_issue_date,
          fingerprintCode: v.fingerprint_code,

          monthlyIncome: v.monthly_income,
          occupationType: v.occupation_type,

          // multipart files (Angular HttpClient manda como Blob/File)
          idDocumentFront: this.idFile,
          selfie: this.selfieFile
        })
      );

      this.registrationId = r.registration_id;
      this.otpChannelHint = r.otp_channel_hint;
      this.activeIndex = 1;
    } finally {
      this.busy = false;
    }
  }


  async verifyOtp(): Promise<void> {
    if (!this.registrationId) return;
    this.busy = true;
    try {
      await firstValueFrom(this.onboardingApi.verifyOnboardingContact({
        verifyContactRequestDto: {
          registration_id: this.registrationId,
          otp: this.otpForm.value.otp!
        }
      }));
      this.activeIndex = 2;
    } finally {
      this.busy = false;
    }
  }

  async submitConsents(): Promise<void> {
    if (!this.registrationId) return;
    this.busy = true;
    try {
      await firstValueFrom(this.onboardingApi.registerOnboardingConsents({
        consentsRequestDto: {
          registration_id: this.registrationId,
          accepted: true
          }
      }));
      this.activeIndex = 3;
    } finally {
      this.busy = false;
    }
  }

  async activate(): Promise<void> {
    if (!this.registrationId) return;
    this.busy = true;
    try {
      await firstValueFrom(this.onboardingApi.activateOnboarding({
        activateRequestDto: {
          registration_id: this.registrationId,
          full_name: this.activateForm.value.full_name!,
          tin: this.activateForm.value.tin!,
          birth_date: this.activateForm.value.birth_date!,
          country: this.activateForm.value.country!,
          email: this.contactForm.value.email!,
          phone: this.contactForm.value.phone!
          }
      }));
      await this.router.navigateByUrl('/login');
    } finally {
      this.busy = false;
    }
  }
}
