import { Component, OnInit } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { AuthService } from '../../auth/auth.service';
import { HomeService } from './service/home-service';
import { Router, RouterLink } from '@angular/router';
import { WhoAmIResponse } from './service/home-service-model';

@Component({
  standalone: true,
  imports: [JsonPipe, RouterLink],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class HomeComponent implements OnInit {
  whoami?: WhoAmIResponse;

  constructor(public auth: AuthService, private service: HomeService, private router: Router) {}

  ngOnInit() {
    if (this.auth.isLoggedIn) {
      this.service.whoAmI().subscribe({
        next: r => this.whoami = r,
        error: e => {
          this.logout(); 
          this.router.navigate(['/']);
        }
      });
    }
  }

  logout(){ this.auth.logout(); }
}
