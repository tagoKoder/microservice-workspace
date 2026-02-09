import { Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth/auth.service';


@Component({
  standalone: true,
  selector: 'app-login-page',
  imports: [CardModule, ButtonModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class Login {
  constructor(private auth: AuthService, private route: ActivatedRoute) {}

  login(): void {
    // post-login -> /home
    const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/home';
    this.auth.startLogin(redirect);
  }
}
