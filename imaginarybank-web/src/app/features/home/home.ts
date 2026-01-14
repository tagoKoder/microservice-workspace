import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { AsyncPipe, JsonPipe } from '@angular/common';

import { AuthService } from '../../core/auth/auth.service';
import { AccountsApi } from '../../api/bff';

@Component({
  standalone: true,
  selector: 'app-home-page',
  imports: [CardModule, ButtonModule, AsyncPipe, JsonPipe],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class Home {
  session$;
  accounts$;

  constructor(
    private auth: AuthService,
    private accountsApi: AccountsApi
  ) {
    this.session$ = this.auth.getSession();
    this.accounts$ = this.accountsApi.getAccountsOverview();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
