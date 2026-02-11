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

import { HttpClient } from '@angular/common/http';

import { AuthService } from '../../core/auth/auth.service';
import { AccountsApi, AccountsOverviewResponseDto, AccountItemDto } from '../../api/bff';

type RecentTx = {
  when: Date;
  type: 'transfer' | 'credit' | 'debit';
  amount: number;
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

  // cache para UI
  accountsCache: AccountItemDto[] = [];

  // selección
  selectedAccountId: string | null = null;
  private carouselIndex = 0;

  // transfer UI
  transferOpen = false;
  transferFromId: string | null = null;
  transferToId: string | null = null;
  transferAmount: number | null = null;
  transferMemo = '';

  // movimientos (mock por ahora)
  recentTx: RecentTx[] = [];

  constructor(
    private auth: AuthService,
    private accountsApi: AccountsApi,
    private http: HttpClient,
    private msg: MessageService
  ) {
    this.session$ = this.auth.getSession();
    this.accounts$ = this.accountsApi.getAccountsOverview().pipe(
  tap(res => {
    this.accountsCache = res.accounts || [];
    // set defaults...
  }),
  shareReplay({ bufferSize: 1, refCount: true }),
  catchError(err => {
    this.toastError('Error', 'No se pudieron cargar las cuentas.');
    return of({ accounts: [] } as AccountsOverviewResponseDto);
  })
);
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }

  private async hydrateAccountsCache(): Promise<void> {
    try {
      const res: AccountsOverviewResponseDto = await firstValueFrom(this.accounts$);
      this.accountsCache = res.accounts || [];

      if (this.accountsCache.length > 0) {
        const first = this.accountsCache[0];
        this.selectedAccountId = first.id;
        this.transferFromId = first.id;
        this.transferToId = this.accountsCache.length > 1 ? this.accountsCache[1].id : first.id;
      }
    } catch {
      this.toastError('Error', 'No se pudieron cargar las cuentas.');
    }
  }

  onCarouselPage(e: any) {
    this.carouselIndex = e?.page ?? 0;
    const a = this.accountsCache?.[this.carouselIndex];
    if (a) {
      this.selectedAccountId = a.id;
      if (!this.transferOpen) this.transferFromId = a.id;
    }
  }

  // select nativo: al cambiar, sincroniza "cuenta activa"
  onSelectAccountChange() {
    const idx = this.accountsCache.findIndex(a => a.id === this.selectedAccountId);
    if (idx >= 0) {
      this.carouselIndex = idx;
      this.transferFromId = this.selectedAccountId;
      // auto-ajuste de destino si quedó igual
      if (this.transferToId === this.transferFromId) {
        const alt = this.accountsCache.find(a => a.id !== this.transferFromId);
        this.transferToId = alt ? alt.id : this.transferFromId;
      }
    }
  }

  prevAccount() {
    const accounts = this.accountsCache;
    if (!accounts?.length) return;
    this.carouselIndex = (this.carouselIndex - 1 + accounts.length) % accounts.length;
    this.selectedAccountId = accounts[this.carouselIndex].id;
    this.transferFromId = this.selectedAccountId;
  }

  nextAccount() {
    const accounts = this.accountsCache;
    if (!accounts?.length) return;
    this.carouselIndex = (this.carouselIndex + 1) % accounts.length;
    this.selectedAccountId = accounts[this.carouselIndex].id;
    this.transferFromId = this.selectedAccountId;
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

    // (opcional UI) validar que "available" sea suficiente (solo UI, el backend manda)
    const available = this.toNumber(from.balances?.available);
    if (available < Number(this.transferAmount)) return false;

    return true;
  }

  async submitTransfer(): Promise<void> {
    const from = this.accountsCache.find(a => a.id === this.transferFromId);
    const to = this.accountsCache.find(a => a.id === this.transferToId);
    if (!from || !to) return;

    const payload = {
      source_account_id: this.transferFromId,
      destination_account_id: this.transferToId,
      amount: String(this.transferAmount), // 👈 mejor enviar string decimal si tu backend usa decimal
      currency: from.currency,
      memo: this.transferMemo || null
    };

    try {
      await firstValueFrom(this.http.post('/api/v1/transfers', payload));

      this.recentTx.unshift({
        when: new Date(),
        type: 'transfer',
        amount: Number(this.transferAmount),
        currency: from.currency,
        status: 'pending'
      });

      this.toastSuccess('Transferencia enviada', 'La operación fue enviada al BFF para procesamiento.');
      this.transferOpen = false;
    } catch (e: any) {
      const msg =
        e?.status === 404
          ? 'El endpoint /api/v1/transfers no existe en el BFF. Ajusta la ruta o implementa el handler.'
          : 'No se pudo enviar la transferencia. Revisa logs del BFF/microservicios.';
      this.toastError('Error', msg);
    }
  }

  scrollToMovements() {
    const el = document.getElementById('movements');
    el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  getSelectedAccount(): AccountItemDto | null {
    if (!this.accountsCache?.length || !this.selectedAccountId) return null;
    return this.accountsCache.find(a => a.id === this.selectedAccountId) || null;
  }

  // DTO trae string decimal -> UI number
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
