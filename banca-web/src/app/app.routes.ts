import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home';
import { CallbackComponent } from './components/callback/callback';
import { authGuard } from './auth/auth.guard';
import { InviteStaffComponent } from './components/invite-staff/invite-staff';
import { AcceptInvitationComponent } from './components/accept-invitation/accept-invitation';

export const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [authGuard] },
  { path: 'callback', component: CallbackComponent},
    { path: 'admin/invite', canActivate: [authGuard], component: InviteStaffComponent },
  { path: 'accept-invite', component: AcceptInvitationComponent },

  { path: '**', redirectTo: '' },
];
