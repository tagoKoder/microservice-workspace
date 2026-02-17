import { Component } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, firstValueFrom, of, shareReplay, tap } from 'rxjs';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CarouselModule } from 'primeng/carousel';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DividerModule } from 'primeng/divider';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { AccordionModule } from 'primeng/accordion';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';

import { AuthService } from '../../core/auth/auth.service';
import {
  AccountsApi,
  AccountsOverviewResponseDto,
  AccountItemDto,
  PaymentsApi,
  SessionApi,
  CreatePaymentRequestDto,
  CreatePaymentResponseDto
} from '../../api/bff';

type RecentTx = {
  when: Date;
  memo?: string | null;
  journalId?: string;
  paymentId?: string;
  type: 'transfer' | 'credit' | 'debit';
  amount: number | null;
  currency: string;
  status: 'posted' | 'pending';
};

@Component({
  standalone: true,
  selector: 'app-home-page',
  imports: [
    CommonModule,
    AsyncPipe,
    FormsModule,
    CardModule,
    ButtonModule,
    TableModule,
    CarouselModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    DividerModule,
    TagModule,
    ToastModule,
    AccordionModule,
    SkeletonModule
  ],
  providers: [MessageService],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class Home {
  session$;
  accounts$;

  accountsCache: AccountItemDto[] = [];

  selectedAccountId: string | null = null;
  private carouselIndex = 0;

  transferOpen = false;
  transferFromId: string | null = null;
  transferToId: string | null = null;
  transferAmount: number | null = null;
  transferMemo = '';

  submittingTransfer = false;

  recentTx: RecentTx[] = [];

  // ✅ defaults para statement
  statementDaysBack = 30;
  includeCounterparty = true;

  constructor(
    private auth: AuthService,
    private accountsApi: AccountsApi,
    private sessionApi: SessionApi,
    private paymentsApi: PaymentsApi,
    private msg: MessageService
  ) {
    this.session$ = this.auth.getSession();

    this.accounts$ = this.accountsApi.getAccountsOverview().pipe(
      tap(res => {
        this.accountsCache = res.accounts || [];

        if (!this.selectedAccountId && this.accountsCache.length > 0) {
          const first = this.accountsCache[0];
          this.selectedAccountId = first.id;
          this.transferFromId = first.id;
          this.transferToId = this.accountsCache.length > 1 ? this.accountsCache[1].id : first.id;

          void this.loadStatement(first.id); // ✅ statement
        }
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
      catchError(() => {
        this.toastError('Error', 'No se pudieron cargar las cuentas.');
        return of({ accounts: [] } as AccountsOverviewResponseDto);
      })
    );
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }

  onCarouselPage(e: any) {
    this.carouselIndex = e?.page ?? 0;
    const a = this.accountsCache?.[this.carouselIndex];
    if (!a) return;

    this.selectedAccountId = a.id;
    if (!this.transferOpen) this.transferFromId = a.id;

    void this.loadStatement(a.id); // ✅ recarga statement al cambiar cuenta
  }

  onSelectAccountChange() {
    const idx = this.accountsCache.findIndex(a => a.id === this.selectedAccountId);
    if (idx < 0) return;

    this.carouselIndex = idx;
    this.transferFromId = this.selectedAccountId;

    if (this.transferToId === this.transferFromId) {
      const alt = this.accountsCache.find(a => a.id !== this.transferFromId);
      this.transferToId = alt ? alt.id : this.transferFromId;
    }

    void this.loadStatement(this.selectedAccountId!); // ✅
  }

  prevAccount() {
    const accounts = this.accountsCache;
    if (!accounts?.length) return;

    this.carouselIndex = (this.carouselIndex - 1 + accounts.length) % accounts.length;
    this.selectedAccountId = accounts[this.carouselIndex].id;
    this.transferFromId = this.selectedAccountId;

    void this.loadStatement(this.selectedAccountId); // ✅
  }

  nextAccount() {
    const accounts = this.accountsCache;
    if (!accounts?.length) return;

    this.carouselIndex = (this.carouselIndex + 1) % accounts.length;
    this.selectedAccountId = accounts[this.carouselIndex].id;
    this.transferFromId = this.selectedAccountId;

    void this.loadStatement(this.selectedAccountId); // ✅
  }

  openTransfer() {
    if (this.accountsCache.length > 0) {
      if (!this.transferFromId) this.transferFromId = this.accountsCache[0].id;
      if (!this.transferToId) {
        this.transferToId = this.accountsCache.length > 1 ? this.accountsCache[1].id : this.accountsCache[0].id;
      }
    }
    this.transferAmount = null;
    this.transferMemo = '';
    this.transferOpen = true;
  }

  canSubmitTransfer(): boolean {
    if (!this.transferFromId || !this.transferToId) return false;
    if (this.transferFromId === this.transferToId) return false;
    if (!this.transferAmount || this.transferAmount <= 0) return false;

    const from = this.accountsCache.find(a => a.id === this.transferFromId);
    const to = this.accountsCache.find(a => a.id === this.transferToId);
    if (!from || !to) return false;

    if ((from.currency || '').toUpperCase() !== (to.currency || '').toUpperCase()) return false;

    const available = this.toNumber(from.balances?.available);
    if (available < Number(this.transferAmount)) return false;

    return true;
  }

  private async getCsrf(): Promise<string> {
    const r = await firstValueFrom(this.sessionApi.getWebCsrfToken());
    return r.csrf_token;
  }

  private newIdempotencyKey(): string {
    return (globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`).toString();
  }

  async submitTransfer(): Promise<void> {
    if (this.submittingTransfer) return;
    if (!this.canSubmitTransfer()) return;

    const from = this.accountsCache.find(a => a.id === this.transferFromId);
    const to = this.accountsCache.find(a => a.id === this.transferToId);
    if (!from || !to) return;

    this.submittingTransfer = true;

    const csrf = await this.getCsrf();
    const idem = this.newIdempotencyKey();

    const payload: CreatePaymentRequestDto = {
      source_account: this.transferFromId!,
      destination_account: this.transferToId!,
      currency: (from.currency || 'USD').toUpperCase(),
      amount: Number(this.transferAmount).toFixed(2),
      memo: this.transferMemo || null
    };

    try {
      const res: CreatePaymentResponseDto = await firstValueFrom(
        this.paymentsApi.executePayment({
          idempotencyKey: idem,
          xCSRFToken: csrf,
          createPaymentRequestDto: payload
        })
      );

      // ✅ refresca statement para ver el movimiento real
      await this.loadStatement(this.transferFromId!);

      this.toastSuccess('Transferencia enviada', `Payment ID: ${this.maskId(res.payment_id)}`);
      this.transferOpen = false;
    } catch (e: any) {
      this.toastError('Error', 'No se pudo enviar la transferencia. Revisa logs del BFF/microservicios.');
    } finally {
      this.submittingTransfer = false;
    }
  }

  // ✅ NUEVO: usa /accounts/{id}/statement con from/to/include_counterparty
  async loadStatement(accountId: string): Promise<void> {
    try {
      const to = new Date();
      const from = new Date(Date.now() - this.statementDaysBack * 24 * 60 * 60 * 1000);

      const res = await firstValueFrom(
        this.accountsApi.getAccountStatement({
          id: accountId,
          from: from.toISOString(), // RFC3339
          to: to.toISOString(),     // RFC3339
          page: 1,
          size: 20,
          includeCounterparty: this.includeCounterparty
        })
      );

      this.recentTx = (res.items || []).map(it => {
        const dir = String((it as any).direction || '').toLowerCase(); // debit|credit
        const kind = String((it as any).kind || '').toLowerCase();     // transfer|...
        const cur = ((it as any).currency || this.accountsCache.find(a => a.id === accountId)?.currency || 'USD').toUpperCase();

        const amt = this.toNumber((it as any).amount);
        const signedAmount = dir === 'debit' ? -amt : amt;

        const cp = (it as any).counterparty;
        const memo =
          (it as any).memo ??
          (cp?.display_name ? `${cp.display_name}${cp?.account_number ? ' · ' + cp.account_number : ''}` : null);

        return {
          when: new Date((it as any).booked_at),
          journalId: (it as any).journal_id,
          memo,
          type: kind === 'transfer' ? 'transfer' : (dir === 'credit' ? 'credit' : 'debit'),
          amount: Number.isFinite(signedAmount) ? signedAmount : null,
          currency: cur,
          status: 'posted'
        } as RecentTx;
      });
    } catch {
      this.toastError('Error', 'No se pudieron cargar los movimientos (statement).');
    }
  }

  scrollToMovements() {
    document.getElementById('movements')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  getSelectedAccount(): AccountItemDto | null {
    if (!this.accountsCache?.length || !this.selectedAccountId) return null;
    return this.accountsCache.find(a => a.id === this.selectedAccountId) || null;
  }

  private toNumber(v: any): number {
    if (v === null || v === undefined) return 0;
    const n = Number(String(v).replace(',', '.'));
    return Number.isFinite(n) ? n : 0;
  }

  formatMoney(amount: any, currency: string): string {
    const n = this.toNumber(amount);
    const cur = (currency || 'USD').toUpperCase();
    try {
      return new Intl.NumberFormat('es-EC', { style: 'currency', currency: cur }).format(n);
    } catch {
      return `${n.toFixed(2)} ${cur}`;
    }
  }

  maskId(v: any): string {
    const s = String(v ?? '');
    if (!s) return '-';
    if (s.length <= 10) return s;
    return `${s.slice(0, 6)}…${s.slice(-4)}`;
  }

  toastSuccess(summary: string, detail: string) {
    this.msg.add({ severity: 'success', summary, detail });
  }
  toastInfo(summary: string, detail: string) {
    this.msg.add({ severity: 'info', summary, detail });
  }
  toastError(summary: string, detail: string) {
    this.msg.add({ severity: 'error', summary, detail });
  }
}