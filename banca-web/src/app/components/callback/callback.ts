import { Component, OnInit } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { CallbackService } from './service/callback-service';

@Component({
  standalone: true,
  imports: [JsonPipe],
  template: `
    <section class="p-6">
      <h2>Procesando inicio de sesión…</h2>
      @if(ready){
        <h3>Claims</h3>
        <pre>{{ auth.claims | json }}</pre>
        <a routerLink="/">Ir a Home</a>
      }
    </section>
  `
})
export class CallbackComponent implements OnInit {
  ready = false;
  constructor(public auth: AuthService, private router: Router, private service: CallbackService) {}

  async ngOnInit() {
    await this.auth.finishLoginOnCallback();

    // Llamamos LINK sólo una vez por sesión
    const alreadyLinked = sessionStorage.getItem('ak_linked') === '1';
    const id = this.auth.idToken;

    if (!alreadyLinked && id) {
      try {
        await this.service.link(id).toPromise();
        sessionStorage.setItem('ak_linked', '1');
        console.log('LINK ok');
      } catch (e) {
        console.warn('LINK error', e);
      }
    }

    this.ready = true;
    // Opcional: navega directo al home
    // this.router.navigateByUrl('/');
  }
}
