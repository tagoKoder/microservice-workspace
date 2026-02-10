import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { AsyncPipe } from '@angular/common';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../core/auth/auth.service';
import { AccountsApi } from '../../api/bff';

@Component({
  standalone: true,
  selector: 'app-home-page',
  imports: [CommonModule, CardModule, ButtonModule, TableModule, AsyncPipe],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class Home {
  session$; // <-- usa cache
  accounts$;

  constructor(
    private auth: AuthService,
    private accountsApi: AccountsApi
  ) {
    this.accounts$ = this.accountsApi.getAccountsOverview();
    this.session$ = this.auth.getSession();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
